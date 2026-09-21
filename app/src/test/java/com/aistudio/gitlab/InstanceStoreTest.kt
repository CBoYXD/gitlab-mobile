package com.aistudio.gitlab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.storage.InstanceStore
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
}
