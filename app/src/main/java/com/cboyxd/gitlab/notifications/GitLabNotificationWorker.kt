package com.cboyxd.gitlab.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cboyxd.gitlab.data.api.GitLabApiException
import com.cboyxd.gitlab.data.api.parseGitLabMillis
import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.data.auth.GitLabSessionTokens
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabTodo
import com.cboyxd.gitlab.data.repository.GitLabRepository
import com.cboyxd.gitlab.data.storage.InstanceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GitLabNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result = try {
    if (GitLabNotificationMonitor.poll(applicationContext)) Result.success() else Result.retry()
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (_: Exception) {
    Result.retry()
  }
}

/** Shared by periodic work and the foreground loop. First successful reads establish a baseline. */
object GitLabNotificationMonitor {
  private val lock = Mutex()
  private val repository = GitLabRepository()

  suspend fun poll(context: Context): Boolean = withContext(Dispatchers.IO) {
    if (!lock.tryLock()) return@withContext true
    try {
      GitLabNotifications.createChannels(context)
      val store = InstanceStore.get(context)
      val accounts = store.getInstances().filter { it.token.isNotBlank() }
      val notifications = NotificationStore(context)
      notifications.prune(accounts.map { it.id }.toSet())
      // Clear notifications for accounts removed while other accounts remain signed in.
      val manager = context.getSystemService(android.app.NotificationManager::class.java)
      if (android.os.Build.VERSION.SDK_INT >= 23) manager.activeNotifications.forEach { notification ->
        if (notification.tag?.substringBefore(':') !in accounts.map { it.id }) manager.cancel(notification.tag, notification.id)
      }
      var success = true
      for (account in accounts) {
        try {
          if (!pollAccount(context, store, notifications, account)) success = false
        } catch (cancel: CancellationException) {
          throw cancel
        } catch (_: Exception) {
          success = false
        }
      }
      success
    } finally {
      lock.unlock()
    }
  }

  private suspend fun <T> api(store: InstanceStore, account: GitLabInstance, block: suspend (GitLabInstance) -> kotlin.Result<T>): kotlin.Result<T> {
    return try {
      val current = GitLabSessionTokens.fresh(store, account.id)
        ?: return kotlin.Result.failure(IllegalStateException("Account removed"))
      val result = block(current)
      if ((result.exceptionOrNull() as? GitLabApiException)?.code == 401 && current.authKind == GitLabAuth.KIND_OAUTH) {
        val renewed = GitLabSessionTokens.fresh(store, account.id, force = true, staleToken = current.token)
        if (renewed != null && renewed.token != current.token) block(renewed) else result
      } else result
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (error: Exception) {
      kotlin.Result.failure(error)
    }
  }

  private suspend fun pollAccount(context: Context, store: InstanceStore, saved: NotificationStore, account: GitLabInstance): Boolean = coroutineScope {
    val started = System.currentTimeMillis()
    val since = saved.cursor(account.id, "authored")
    val todos = async { api(store, account) { repository.getTodos(it) } }
    val assigned = async { api(store, account) { repository.getNotificationMergeRequests(it, "assigned_to_me") } }
    val reviews = async { api(store, account) { repository.getNotificationMergeRequests(it, "reviews_for_me") } }
    val authored = async { api(store, account) { repository.getNotificationMergeRequests(it, "created_by_me", since.takeIf { it > 0 }?.let { iso(it - 60_000) }) } }
    val assignedResult = assigned.await()
    val reviewResult = reviews.await()
    val todoResult = todos.await()
    val authoredResult = authored.await()
    // Signing out during a network request must not deliver events from that account.
    if (store.getInstances().none { it.id == account.id && it.token.isNotBlank() }) return@coroutineScope true
    assignedResult.getOrNull()?.let { processMembership(context, saved, account, "assigned", it, started) }
    reviewResult.getOrNull()?.let { processMembership(context, saved, account, "review", it, started) }
    todoResult.getOrNull()?.let { list ->
      val todoSince = saved.cursor(account.id, "todos")
      val suppressedAssigned = assignedResult.getOrNull()?.map { mrKey(it) }?.toSet().orEmpty()
      val suppressedReviews = reviewResult.getOrNull()?.map { mrKey(it) }?.toSet().orEmpty()
      if (todoSince > 0) list.sortedBy { it.createdAt }.forEach { todo ->
        if ((parseGitLabMillis(todo.createdAt) ?: 0) < todoSince - 300_000) return@forEach
        val projectId = todo.target?.projectId ?: todo.project?.id ?: 0
        val key = "$projectId:${todo.target?.iid ?: 0}"
        if (todo.targetType == "MergeRequest" && (
            (todo.actionName == "assigned" && key in suppressedAssigned) ||
            (todo.actionName == "review_requested" && key in suppressedReviews)
          )) {
          eventForTodo(account, todo)?.let { saved.record(it) }
          return@forEach
        }
        eventForTodo(account, todo)?.let { event -> deliver(context, saved, event) }
        // If a membership endpoint failed, a to-do still establishes that membership and
        // prevents a duplicate announcement when the endpoint recovers.
        val feed = when {
          todo.targetType != "MergeRequest" -> null
          todo.actionName == "assigned" && assignedResult.isFailure -> "assigned"
          todo.actionName == "review_requested" && reviewResult.isFailure -> "review"
          else -> null
        }
        if (feed != null && projectId > 0) {
          val snapshot = saved.snapshot(account.id, feed)
          snapshot.put(key, true)
          saved.saveSnapshot(account.id, feed, snapshot)
        }
      }
      if (todoSince == 0L) list.forEach { todo -> eventForTodo(account, todo)?.let { saved.record(it) } }
      saved.advance(account.id, "todos", started)
    }
    authoredResult.getOrNull()?.let { list ->
      val previous = saved.snapshot(account.id, "authored")
      if (since > 0) list.sortedBy { it.updatedAt }.forEach { mr ->
        val old = previous.optString(mrKey(mr))
        val changedAt = parseGitLabMillis(if (mr.state == "merged") mr.mergedAt.orEmpty() else mr.closedAt ?: mr.updatedAt) ?: 0
        val title = when {
          mr.state == "merged" && (old.isNotBlank() && old != "merged" || old.isBlank() && changedAt >= since) -> "Your merge request was merged"
          mr.state == "closed" && (old.isNotBlank() && old != "closed" || old.isBlank() && changedAt >= since) -> "Your merge request was closed"
          mr.state == "opened" && old in setOf("closed", "merged") -> "Your merge request was reopened"
          else -> null
        }
        if (title != null) {
          val actor = (if (mr.state == "merged") mr.mergeUser ?: mr.mergedBy else mr.closedBy)?.let { it.name.ifBlank { it.username } }
          deliver(context, saved, mrEvent(account, mr, "state:${mr.state}:${mr.mergedAt ?: mr.closedAt ?: mr.updatedAt}", title, GitLabNotifications.ACTIVITY, actor))
        }
      }
      list.forEach { previous.put(mrKey(it), it.state) }
      while (previous.length() > 2000) previous.remove(previous.keys().next())
      saved.saveSnapshot(account.id, "authored", previous)
      saved.advance(account.id, "authored", started)
    }
    listOf(assignedResult, reviewResult, todoResult, authoredResult).none { result ->
      val error = result.exceptionOrNull()
      error != null && (error !is GitLabApiException || error.code == 429 || error.code >= 500)
    }
  }

  private fun processMembership(context: Context, saved: NotificationStore, account: GitLabInstance, feed: String, list: List<GitLabMergeRequest>, started: Long) {
    val old = saved.snapshot(account.id, feed)
    if (saved.cursor(account.id, feed) > 0) list.forEach { mr ->
      if (!old.has(mrKey(mr))) {
        deliver(context, saved, mrEvent(account, mr, "$feed:${mr.updatedAt}", if (feed == "review") "Review requested from you" else "Merge request assigned to you", GitLabNotifications.REVIEWS))
      }
    }
    val next = JSONObject()
    list.forEach { next.put(mrKey(it), true) }
    saved.saveSnapshot(account.id, feed, next)
    saved.advance(account.id, feed, started)
  }

  private fun deliver(context: Context, saved: NotificationStore, event: GitLabNotificationEvent) {
    if (saved.record(event)) GitLabNotifications.show(context, event)
  }

  private fun mrKey(mr: GitLabMergeRequest) = "${mr.projectId}:${mr.iid}"

  private fun mrEvent(account: GitLabInstance, mr: GitLabMergeRequest, suffix: String, title: String, channel: String, actor: String? = null): GitLabNotificationEvent {
    val project = mr.webUrl.substringBefore("/-/").substringAfter("://").substringAfter('/')
    return GitLabNotificationEvent("mr:${mrKey(mr)}:$suffix", account.id, title,
      listOfNotNull("!${mr.iid} ${mr.title}", project.takeIf { it.isNotBlank() }, actor?.takeIf { it.isNotBlank() }?.let { "By $it" }, account.name).joinToString("\n"),
      "MergeRequest", mr.projectId, mr.iid, mr.webUrl, channel)
  }

  private fun eventForTodo(account: GitLabInstance, todo: GitLabTodo): GitLabNotificationEvent? {
    val title = when (todo.actionName) {
      "assigned" -> if (todo.targetType == "MergeRequest") "Merge request assigned to you" else "Issue assigned to you"
      "review_requested" -> "Review requested from you"
      "review_submitted" -> "A review was submitted"
      "mentioned", "directly_addressed" -> "You were mentioned"
      "approval_required" -> "Your approval is required"
      "build_failed" -> "Pipeline failed"
      "unmergeable" -> "Merge request has conflicts"
      "merge_train_removed" -> "Merge request removed from merge train"
      else -> return null
    }
    val targetTitle = todo.target?.title ?: todo.body?.lineSequence()?.firstOrNull().orEmpty()
    val channel = when (todo.actionName) {
      "assigned", "review_requested", "approval_required" -> GitLabNotifications.REVIEWS
      "build_failed" -> GitLabNotifications.PIPELINES
      else -> GitLabNotifications.ACTIVITY
    }
    val number = todo.target?.iid?.takeIf { it > 0 }?.let { "${if (todo.targetType == "MergeRequest") "!" else "#"}$it " }.orEmpty()
    return GitLabNotificationEvent("todo:${todo.id}", account.id, title,
      listOf("$number$targetTitle", todo.project?.pathWithNamespace.orEmpty(), "From ${todo.author.name.ifBlank { todo.author.username }} · ${account.name}").filter { it.isNotBlank() }.joinToString("\n"),
      todo.targetType.orEmpty(), todo.target?.projectId ?: todo.project?.id ?: 0, todo.target?.iid ?: 0, todo.targetUrl.orEmpty(), channel,
      parseGitLabMillis(todo.createdAt) ?: System.currentTimeMillis())
  }

  private fun iso(time: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(time))
}
