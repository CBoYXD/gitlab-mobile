package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GitLab", appName)
  }

  @Test
  fun `instance store manages custom instances`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.storage.InstanceStore(context)
    val defaultActive = store.getActiveInstance()
    assertEquals("https://gitlab.com", defaultActive.url)

    val customInstance = com.example.data.model.GitLabInstance(
      name = "Company On-Prem",
      url = "https://gitlab.internal.corp",
      token = "glpat-test-12345",
      isCustom = true
    )
    store.addInstance(customInstance)
    store.setActiveInstance(customInstance.id)

    val updatedActive = store.getActiveInstance()
    assertEquals("Company On-Prem", updatedActive.name)
    assertEquals("https://gitlab.internal.corp", updatedActive.url)
  }
}
