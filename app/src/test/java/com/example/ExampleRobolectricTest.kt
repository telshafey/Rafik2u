package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.RafikNotificationManager
import com.example.notifications.ReminderSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    assertEquals("رفيق", appName)
  }

  @Test
  fun `notification channel and reminder settings persist cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    RafikNotificationManager.ensureNotificationChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertNotNull(manager.getNotificationChannel(RafikNotificationManager.CHANNEL_ID))

    RafikNotificationManager.saveAndApplySetting(
      context = context,
      slot = ReminderSlot.QURAN_REVISION,
      enabled = true,
      hour = 21,
      minute = 15
    )
    val saved = RafikNotificationManager.loadAllSettings(context)
      .first { it.slot == ReminderSlot.QURAN_REVISION }
    assertTrue(saved.enabled)
    assertEquals(21, saved.hour)
    assertEquals(15, saved.minute)
  }
}
