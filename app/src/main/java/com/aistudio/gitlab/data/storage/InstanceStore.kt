package com.aistudio.gitlab.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.gitlab.data.model.GitLabInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Saved servers, encrypted with an Android Keystore key. Reads are served from memory after the
 * first decrypt; every method blocks on disk or Keystore, so call it off the main thread.
 */
class InstanceStore(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val secretStore = AndroidKeyStoreSecretStore(context, SECRET_PREFS_NAME)

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val listType = Types.newParameterizedType(List::class.java, GitLabInstance::class.java)
  private val adapter = moshi.adapter<List<GitLabInstance>>(listType)

  private var cache: List<GitLabInstance>? = null

  @Synchronized
  fun getInstances(): List<GitLabInstance> {
    cache?.let { return it }
    val json = secretStore.get(KEY_INSTANCES) ?: prefs.getString(KEY_INSTANCES, null)?.also { legacy ->
      secretStore.put(KEY_INSTANCES, legacy)
      prefs.edit().remove(KEY_INSTANCES).commit()
    }
    if (json == null) {
      cache = emptyList()
      return emptyList()
    }
    val parsed = try {
      adapter.fromJson(json).orEmpty()
    } catch (_: Exception) {
      emptyList()
    }
    val cleaned = parsed.filterNot { it.isSeededDemo() }
    if (cleaned.size != parsed.size) saveInstances(cleaned) else cache = cleaned
    return cleaned
  }

  @Synchronized
  fun saveInstances(instances: List<GitLabInstance>) {
    secretStore.put(KEY_INSTANCES, adapter.toJson(instances))
    prefs.edit().remove(KEY_INSTANCES).commit()
    cache = instances
  }

  @Synchronized
  fun getActiveInstance(): GitLabInstance? {
    val instances = getInstances()
    if (instances.isEmpty()) return null
    val activeId = prefs.getString(KEY_ACTIVE_ID, null)
    return instances.firstOrNull { it.id == activeId }
      ?: instances.firstOrNull { it.isActive }
      ?: instances.first()
  }

  @Synchronized
  fun setActiveInstance(instanceId: String) {
    val instances = getInstances().map { it.copy(isActive = it.id == instanceId) }
    saveInstances(instances)
    prefs.edit().putString(KEY_ACTIVE_ID, instanceId).commit()
  }

  /**
   * Adds a server, or replaces the saved one with the same URL and username so signing in again
   * refreshes the credentials instead of creating a duplicate entry.
   */
  @Synchronized
  fun addInstance(instance: GitLabInstance): GitLabInstance {
    val current = getInstances()
    val existing = current.firstOrNull { it.sameAccountAs(instance) }
    val active = instance.isActive || current.isEmpty()
    val toSave = instance.copy(id = existing?.id ?: instance.id, isActive = active)
    val others = current.filterNot { it.id == toSave.id }
    val updated = if (active) others.map { it.copy(isActive = false) } + toSave else others + toSave
    saveInstances(updated)
    if (active) prefs.edit().putString(KEY_ACTIVE_ID, toSave.id).commit()
    return toSave
  }

  /** Updates a saved server. Returns false when it was deleted meanwhile. */
  @Synchronized
  fun updateInstance(instance: GitLabInstance): Boolean {
    val current = getInstances()
    if (current.none { it.id == instance.id }) return false
    saveInstances(current.map { if (it.id == instance.id) instance.copy(isActive = it.isActive) else it })
    return true
  }

  fun getGitLabComClientId(): String = prefs.getString(KEY_OAUTH_CLIENT_ID, "").orEmpty()

  fun setGitLabComClientId(clientId: String) {
    prefs.edit().putString(KEY_OAUTH_CLIENT_ID, clientId.trim()).apply()
  }

  @Synchronized
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
      prefs.edit().remove(KEY_ACTIVE_ID).commit()
    } else if (prefs.getString(KEY_ACTIVE_ID, null) == instanceId) {
      prefs.edit().putString(KEY_ACTIVE_ID, finalList.first { it.isActive }.id).commit()
    }
  }

  private fun GitLabInstance.sameAccountAs(other: GitLabInstance): Boolean {
    val sameUrl = url.trim().trimEnd('/').equals(other.url.trim().trimEnd('/'), ignoreCase = true)
    if (!sameUrl) return false
    // One public (no sign-in) entry per server.
    if (token.isBlank() && other.token.isBlank()) return true
    if (username.isBlank() || other.username.isBlank()) return false
    return username.equals(other.username, ignoreCase = true)
  }

  private fun GitLabInstance.isSeededDemo(): Boolean {
    if (token.isNotBlank()) return false
    return id in SEEDED_IDS && username in SEEDED_USERS
  }

  private companion object {
    const val PREFS_NAME = "gitlab_mobile_prefs"
    const val SECRET_PREFS_NAME = "gitlab_mobile_secrets"
    const val KEY_INSTANCES = "saved_instances"
    const val KEY_ACTIVE_ID = "active_instance_id"
    const val KEY_OAUTH_CLIENT_ID = "gitlab_com_oauth_client_id"
    val SEEDED_IDS = setOf("gitlab_com", "gnome_gitlab", "freedesktop_gitlab")
    val SEEDED_USERS = setOf("tanuki_developer", "gnome_contributor", "desktop_dev")
  }
}
