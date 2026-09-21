package com.aistudio.gitlab.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.gitlab.data.model.GitLabInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class InstanceStore(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val listType = Types.newParameterizedType(List::class.java, GitLabInstance::class.java)
  private val adapter = moshi.adapter<List<GitLabInstance>>(listType)

  fun getInstances(): List<GitLabInstance> {
    val json = prefs.getString(KEY_INSTANCES, null) ?: return emptyList()
    val parsed = try {
      adapter.fromJson(json).orEmpty()
    } catch (_: Exception) {
      emptyList()
    }
    val cleaned = parsed.filterNot { it.isSeededDemo() }
    if (cleaned.size != parsed.size) saveInstances(cleaned)
    return cleaned
  }

  fun saveInstances(instances: List<GitLabInstance>) {
    prefs.edit().putString(KEY_INSTANCES, adapter.toJson(instances)).apply()
  }

  fun getActiveInstance(): GitLabInstance? {
    val instances = getInstances()
    if (instances.isEmpty()) return null
    val activeId = prefs.getString(KEY_ACTIVE_ID, null)
    return instances.firstOrNull { it.id == activeId }
      ?: instances.firstOrNull { it.isActive }
      ?: instances.first()
  }

  fun setActiveInstance(instanceId: String) {
    val instances = getInstances().map { it.copy(isActive = it.id == instanceId) }
    saveInstances(instances)
    prefs.edit().putString(KEY_ACTIVE_ID, instanceId).apply()
  }

  fun addInstance(instance: GitLabInstance) {
    val current = getInstances()
    val active = instance.isActive || current.isEmpty()
    val toSave = instance.copy(isActive = active)
    val updated = if (active) current.map { it.copy(isActive = false) } + toSave else current + toSave
    saveInstances(updated)
    if (active) prefs.edit().putString(KEY_ACTIVE_ID, toSave.id).apply()
  }

  fun updateInstance(instance: GitLabInstance) {
    val updated = getInstances().map { if (it.id == instance.id) instance else it }
    if (updated.none { it.id == instance.id }) return
    saveInstances(updated)
  }

  fun deleteInstance(instanceId: String) {
    val remaining = getInstances().filter { it.id != instanceId }
    val finalList = when {
      remaining.isEmpty() -> emptyList()
      remaining.none { it.isActive } -> remaining.mapIndexed { index, instance ->
        if (index == 0) instance.copy(isActive = true) else instance
      }
      else -> remaining
    }
    saveInstances(finalList)
    if (finalList.isEmpty()) {
      prefs.edit().remove(KEY_ACTIVE_ID).apply()
    } else if (prefs.getString(KEY_ACTIVE_ID, null) == instanceId) {
      prefs.edit().putString(KEY_ACTIVE_ID, finalList.first { it.isActive }.id).apply()
    }
  }

  private fun GitLabInstance.isSeededDemo(): Boolean {
    if (token.isNotBlank()) return false
    return id in SEEDED_IDS && username in SEEDED_USERS
  }

  private companion object {
    const val PREFS_NAME = "gitlab_mobile_prefs"
    const val KEY_INSTANCES = "saved_instances"
    const val KEY_ACTIVE_ID = "active_instance_id"
    val SEEDED_IDS = setOf("gitlab_com", "gnome_gitlab", "freedesktop_gitlab")
    val SEEDED_USERS = setOf("tanuki_developer", "gnome_contributor", "desktop_dev")
  }
}
