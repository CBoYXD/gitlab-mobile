package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.GitLabInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class InstanceStore(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("gitlab_mobile_prefs", Context.MODE_PRIVATE)

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val listType = Types.newParameterizedType(List::class.java, GitLabInstance::class.java)
  private val adapter = moshi.adapter<List<GitLabInstance>>(listType)

  companion object {
    private const val KEY_INSTANCES = "saved_instances"
    private const val KEY_ACTIVE_ID = "active_instance_id"
    private const val KEY_STARRED = "starred_project_ids"

    val DEFAULT_INSTANCES = listOf(
      GitLabInstance(
        id = "gitlab_com",
        name = "GitLab.com",
        url = "https://gitlab.com",
        username = "tanuki_developer",
        isActive = true,
        isCustom = false,
        version = "17.4.0-ee",
        statusOk = true
      ),
      GitLabInstance(
        id = "gnome_gitlab",
        name = "GNOME GitLab",
        url = "https://gitlab.gnome.org",
        username = "gnome_contributor",
        isActive = false,
        isCustom = true,
        version = "16.11-ee",
        statusOk = true
      ),
      GitLabInstance(
        id = "freedesktop_gitlab",
        name = "Freedesktop GitLab",
        url = "https://gitlab.freedesktop.org",
        username = "desktop_dev",
        isActive = false,
        isCustom = true,
        version = "16.10-ee",
        statusOk = true
      )
    )
  }

  fun getInstances(): List<GitLabInstance> {
    val json = prefs.getString(KEY_INSTANCES, null) ?: return DEFAULT_INSTANCES
    return try {
      val list = adapter.fromJson(json)
      if (list.isNullOrEmpty()) DEFAULT_INSTANCES else list
    } catch (e: Exception) {
      DEFAULT_INSTANCES
    }
  }

  fun saveInstances(instances: List<GitLabInstance>) {
    try {
      val json = adapter.toJson(instances)
      prefs.edit().putString(KEY_INSTANCES, json).apply()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getActiveInstance(): GitLabInstance {
    val instances = getInstances()
    val activeId = prefs.getString(KEY_ACTIVE_ID, null)
    return instances.firstOrNull { it.id == activeId }
      ?: instances.firstOrNull { it.isActive }
      ?: instances.first()
  }

  fun setActiveInstance(instanceId: String) {
    val instances = getInstances().map {
      it.copy(isActive = (it.id == instanceId))
    }
    saveInstances(instances)
    prefs.edit().putString(KEY_ACTIVE_ID, instanceId).apply()
  }

  fun addInstance(instance: GitLabInstance) {
    val current = getInstances().toMutableList()
    // If setting active, deactivate others
    val updatedList = if (instance.isActive) {
      current.map { it.copy(isActive = false) }.plus(instance)
    } else {
      current.plus(instance)
    }
    saveInstances(updatedList)
    if (instance.isActive) {
      prefs.edit().putString(KEY_ACTIVE_ID, instance.id).apply()
    }
  }

  fun deleteInstance(instanceId: String) {
    val current = getInstances().filter { it.id != instanceId }
    val finalList = if (current.none { it.isActive } && current.isNotEmpty()) {
      current.mapIndexed { idx, inst -> if (idx == 0) inst.copy(isActive = true) else inst }
    } else current
    saveInstances(finalList)
    if (prefs.getString(KEY_ACTIVE_ID, null) == instanceId && finalList.isNotEmpty()) {
      prefs.edit().putString(KEY_ACTIVE_ID, finalList.first().id).apply()
    }
  }

  fun getStarredProjectIds(): Set<Long> {
    val set = prefs.getStringSet(KEY_STARRED, emptySet()) ?: emptySet()
    return set.mapNotNull { it.toLongOrNull() }.toSet()
  }

  fun toggleStarred(projectId: Long): Boolean {
    val current = getStarredProjectIds().toMutableSet()
    val isNowStarred = if (current.contains(projectId)) {
      current.remove(projectId)
      false
    } else {
      current.add(projectId)
      true
    }
    prefs.edit().putStringSet(KEY_STARRED, current.map { it.toString() }.toSet()).apply()
    return isNowStarred
  }
}
