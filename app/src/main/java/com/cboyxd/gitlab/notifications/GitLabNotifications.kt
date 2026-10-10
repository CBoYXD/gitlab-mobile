package com.cboyxd.gitlab.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cboyxd.gitlab.MainActivity
import com.cboyxd.gitlab.R
import java.util.concurrent.TimeUnit

object GitLabNotifications {
  const val ACTION_OPEN = "com.cboyxd.gitlab.OPEN_NOTIFICATION"
  const val EXTRA_ACCOUNT = "account_id"
  const val EXTRA_TARGET = "target_type"
  const val EXTRA_PROJECT = "project_id"
  const val EXTRA_IID = "target_iid"
  const val EXTRA_URL = "target_url"
  internal const val REVIEWS = "gitlab_reviews"
  internal const val ACTIVITY = "gitlab_activity"
  internal const val PIPELINES = "gitlab_pipelines"
  private const val PERIODIC = "gitlab_notification_poll"
  private const val IMMEDIATE = "gitlab_notification_check"

  fun createChannels(context: Context) {
    if (Build.VERSION.SDK_INT < 26) return
    val manager = context.getSystemService(NotificationManager::class.java)
    listOf(
      Triple(REVIEWS, "Assignments and reviews", "Merge requests assigned to you and requests for review"),
      Triple(ACTIVITY, "GitLab activity", "Your merge requests, mentions and discussions"),
      Triple(PIPELINES, "Pipeline failures", "Failed pipelines that GitLab adds to your to-do list"),
    ).forEach { (id, title, description) ->
      manager.createNotificationChannel(NotificationChannel(id, title, NotificationManager.IMPORTANCE_DEFAULT).apply { this.description = description })
    }
  }

  fun allowed(context: Context): Boolean =
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
      NotificationManagerCompat.from(context).areNotificationsEnabled()

  fun schedule(context: Context, hasAccounts: Boolean) {
    createChannels(context)
    val work = WorkManager.getInstance(context)
    if (!hasAccounts) {
      work.cancelUniqueWork(PERIODIC)
      work.cancelUniqueWork(IMMEDIATE)
      NotificationManagerCompat.from(context).cancelAll()
      return
    }
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
      PeriodicWorkRequestBuilder<GitLabNotificationWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
    work.enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.KEEP,
      OneTimeWorkRequestBuilder<GitLabNotificationWorker>().setConstraints(constraints).build())
  }

  internal fun show(context: Context, event: GitLabNotificationEvent) {
    if (!allowed(context)) return
    val intent = Intent(context, MainActivity::class.java).apply {
      action = ACTION_OPEN
      data = Uri.Builder().scheme("gitlab-mobile").authority("notification").appendPath(event.accountId).appendPath(event.id).build()
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_ACCOUNT, event.accountId)
      putExtra(EXTRA_TARGET, event.targetType)
      putExtra(EXTRA_PROJECT, event.projectId)
      putExtra(EXTRA_IID, event.iid)
      putExtra(EXTRA_URL, event.url)
    }
    val click = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(context, event.channel)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(event.title)
      .setContentText(event.message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
      .setContentIntent(click)
      .setAutoCancel(true)
      .setWhen(event.createdAt)
      .setShowWhen(true)
      .setCategory(NotificationCompat.CATEGORY_SOCIAL)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      .setGroup("gitlab:${event.accountId}")
      .build()
    try {
      NotificationManagerCompat.from(context).notify("${event.accountId}:${event.id}", 0, notification)
    } catch (_: SecurityException) {
      // Permission can be revoked between the check and posting.
    }
  }
}
