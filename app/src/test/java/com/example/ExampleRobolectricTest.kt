package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AppViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("EarnMitra", appName)
  }

  @Test
  fun `test app viewmodel initialization`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = AppViewModel(app)
    assertNotNull(viewModel)
  }

  @Test
  fun `test MainActivity launch`() {
    val activityController = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = activityController.get()
    assertNotNull(activity)
  }
}
