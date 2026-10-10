package com.cboyxd.gitlab.notifications

import android.content.Context
import org.json.JSONObject

internal data class GitLabNotificationEvent(
  val id: String,
  val accountId: String,
  val title: String,
  val message: String,
  val targetType: String,
  val projectId: Long,
  val iid: Long,
  val url: String,
  val channel: String,
  val createdAt: Long = System.currentTimeMillis(),
)

/** Small per-account cursors and a durable deduplication window survive app restarts. */
internal class NotificationStore(context: Context) {
  private val prefs = context.getSharedPreferences("gitlab_notification_state", Context.MODE_PRIVATE)

  fun cursor(account: String, feed: String): Long = prefs.getLong("cursor:$account:$feed", 0)
  fun advance(account: String, feed: String, time: Long) { prefs.edit().putLong("cursor:$account:$feed", time).commit() }
  fun snapshot(account: String, feed: String): JSONObject = runCatching { JSONObject(prefs.getString("snapshot:$account:$feed", "{}")!!) }.getOrDefault(JSONObject())
  fun saveSnapshot(account: String, feed: String, value: JSONObject) { prefs.edit().putString("snapshot:$account:$feed", value.toString()).commit() }

  fun record(event: GitLabNotificationEvent): Boolean {
    val key = "delivered:${event.accountId}"
    val previous = prefs.getStringSet(key, emptySet()).orEmpty()
    if (event.id in previous) return false
    val ids = LinkedHashSet(previous)
    ids += event.id
    // Cursors also prevent older events from being replayed when this bounded window rolls over.
    while (ids.size > 2000) ids.remove(ids.first())
    return prefs.edit().putStringSet(key, ids).commit()
  }

  fun prune(accounts: Set<String>) {
    val editor = prefs.edit()
    prefs.all.keys.forEach { key ->
      val account = key.split(':').getOrNull(1)
      if (account != null && account !in accounts) editor.remove(key)
    }
    editor.apply()
  }
}
