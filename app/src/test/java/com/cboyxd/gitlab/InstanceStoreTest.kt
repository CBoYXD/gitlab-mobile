package com.cboyxd.gitlab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.storage.InstanceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InstanceStoreTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GitLab", appName)
  }

  @Test
  fun `store starts empty and keeps a real instance`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = InstanceStore(context)
    assertNull(store.getActiveInstance())
    assertTrue(store.getInstances().isEmpty())

    val instance = GitLabInstance(
      name = "Company GitLab",
      url = "https://gitlab.company.internal",
      token = "glpat-test",
      username = "dev",
      isCustom = true,
      isActive = true
    )
    store.addInstance(instance)

    assertEquals("Company GitLab", store.getActiveInstance()?.name)
    assertEquals("https://gitlab.company.internal", store.getActiveInstance()?.url)

    store.deleteInstance(instance.id)
    assertNull(store.getActiveInstance())
  }

  @Test
  fun `previously seeded demo instances are dropped`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("gitlab_mobile_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString(
      "saved_instances",
      """[{"id":"gitlab_com","name":"GitLab.com","url":"https://gitlab.com","token":"","username":"tanuki_developer","isActive":true,"isCustom":false,"version":"17.4.0-ee","statusOk":true},{"id":"real","name":"Kept","url":"https://gitlab.company.internal","token":"glpat","username":"dev","isActive":false,"isCustom":true,"version":"","statusOk":true}]"""
    ).commit()

    val store = InstanceStore(context)
    assertEquals(listOf("Kept"), store.getInstances().map { it.name })
    assertEquals("Kept", store.getActiveInstance()?.name)
  }

  @Test
  fun `guest servers saved by older versions are dropped and real accounts kept`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("gitlab_mobile_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString(
      "saved_instances",
      """[{"id":"guest","name":"GitLab.com","url":"https://gitlab.com","token":"","isActive":true,"isCustom":false,"authKind":"public"},{"id":"real","name":"Company","url":"https://gitlab.company.internal","token":"glpat","username":"dev","isActive":false,"isCustom":true,"authKind":"pat"}]"""
    ).commit()

    val store = InstanceStore(context)
    assertEquals(listOf("Company"), store.getInstances().map { it.name })
    assertEquals("Company", store.getActiveInstance()?.name)
    // The cleanup is persisted, not just filtered in memory.
    assertEquals(listOf("Company"), InstanceStore(context).getInstances().map { it.name })
  }

  @Test
  fun `only guest entries leave no active account`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("gitlab_mobile_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString(
      "saved_instances",
      """[{"id":"guest","name":"GitLab.com","url":"https://gitlab.com","token":"","isActive":true,"isCustom":false,"authKind":"public"}]"""
    ).commit()

    val store = InstanceStore(context)
    assertTrue(store.getInstances().isEmpty())
    assertNull(store.getActiveInstance())
  }

  @Test
  fun `legacy credentials migrate out of plaintext preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("gitlab_mobile_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString(
      "saved_instances",
      """[{"id":"real","name":"Kept","url":"https://gitlab.company.internal","token":"glpat-secret","username":"dev","isActive":true,"isCustom":true}]"""
    ).commit()

    val store = InstanceStore(context)
    assertEquals("glpat-secret", store.getActiveInstance()?.token)
    assertNull(prefs.getString("saved_instances", null))
    val encrypted = context.getSharedPreferences("gitlab_mobile_secrets", Context.MODE_PRIVATE)
      .getString("saved_instances", null)
    assertTrue(encrypted != null && !encrypted.contains("glpat-secret"))
  }
}
