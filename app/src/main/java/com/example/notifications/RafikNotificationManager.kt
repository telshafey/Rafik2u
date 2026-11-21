package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

enum class ReminderSlot(
    val id: Int,
    val prefKeyPrefix: String,
    val titleAr: String,
    val defaultHour: Int,
    val defaultMinute: Int,
    val notificationTitle: String,
    val notificationBody: String
) {
    QURAN_REVISION(
        id = 1001,
        prefKeyPrefix = "quran_rev",
        titleAr = "تذكير ورد المراجعة والتعهد",
        defaultHour = 20,
        defaultMinute = 0,
        notificationTitle = "ورد تعهد القرآن ينتظرك 📖",
        notificationBody = "«تعاهدوا هذا القرآن» — راجع وردك اليومي أو خُض اختباراً سريعاً لتثبيت الحفظ."
    ),
    MORNING_AZKAR(
        id = 1002,
        prefKeyPrefix = "morning_azkar",
        titleAr = "أذكار الصباح",
        defaultHour = 6,
        defaultMinute = 30,
        notificationTitle = "وقت أذكار الصباح ☀️",
        notificationBody = "حصّن يومك بأذكار الصباح الصحيحة وابدأ حفظك ومراجعتك بالبركة."
    ),
    EVENING_AZKAR(
        id = 1003,
        prefKeyPrefix = "evening_azkar",
        titleAr = "أذكار المساء",
        defaultHour = 17,
        defaultMinute = 0,
        notificationTitle = "وقت أذكار المساء 🌙",
        notificationBody = "لا تنسَ أذكار المساء وتفقد مهامك اليومية في تطبيق رفيق."
    )
}

data class ReminderSetting(
    val slot: ReminderSlot,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int
)

object RafikNotificationManager {

    const val CHANNEL_ID = "rafik_daily_reminders"
    private const val PREFS_NAME = "rafik_notification_prefs"
    private const val EXTRA_SLOT_NAME = "extra_reminder_slot"
    private const val TEST_NOTIFICATION_ID = 9001

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تذكيرات رفيق اليومية",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "تنبيهات ورد مراجعة القرآن الكريم وأذكار الصباح والمساء"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun loadAllSettings(context: Context): List<ReminderSetting> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSlot.entries.map { slot ->
            ReminderSetting(
                slot = slot,
                enabled = prefs.getBoolean("${slot.prefKeyPrefix}_enabled", false),
                hour = prefs.getInt("${slot.prefKeyPrefix}_hour", slot.defaultHour).coerceIn(0, 23),
                minute = prefs.getInt("${slot.prefKeyPrefix}_minute", slot.defaultMinute).coerceIn(0, 59)
            )
        }
    }

    fun saveAndApplySetting(
        context: Context,
        slot: ReminderSlot,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("${slot.prefKeyPrefix}_enabled", enabled)
            .putInt("${slot.prefKeyPrefix}_hour", hour.coerceIn(0, 23))
            .putInt("${slot.prefKeyPrefix}_minute", minute.coerceIn(0, 59))
            .apply()

        if (enabled) {
            scheduleDailyReminder(appContext, slot, hour, minute)
        } else {
            cancelDailyReminder(appContext, slot)
        }
    }

    fun rescheduleAllEnabled(context: Context) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        loadAllSettings(appContext).forEach { setting ->
            if (setting.enabled) {
                scheduleDailyReminder(appContext, setting.slot, setting.hour, setting.minute)
            }
        }
    }

    private fun scheduleDailyReminder(
        context: Context,
        slot: ReminderSlot,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildReminderPendingIntent(context, slot)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Play-Policy friendly repeating reminder without requiring SCHEDULE_EXACT_ALARM
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelDailyReminder(context: Context, slot: ReminderSlot) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(buildReminderPendingIntent(context, slot))
    }

    private fun buildReminderPendingIntent(context: Context, slot: ReminderSlot): PendingIntent {
        val intent = Intent(context, RafikReminderReceiver::class.java).apply {
            putExtra(EXTRA_SLOT_NAME, slot.name)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showReminderNotification(context: Context, slot: ReminderSlot) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            slot.id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_rafik)
            .setContentTitle(slot.notificationTitle)
            .setContentText(slot.notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(slot.notificationBody))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        NotificationManagerCompat.from(appContext).notify(slot.id, builder.build())
    }

    fun sendTestNotificationNow(context: Context): Boolean {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            TEST_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = "تم تفعيل إشعارات «رفيق» بنجاح! سيصلك التذكير اليومي بموعد المراجعة والأذكار بإذن الله."
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_rafik)
            .setContentTitle("إشعار تجريبي من رفيق 📖")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        NotificationManagerCompat.from(appContext).notify(TEST_NOTIFICATION_ID, builder.build())
        return true
    }

    fun resolveSlotFromIntent(intent: Intent?): ReminderSlot {
        val name = intent?.getStringExtra(EXTRA_SLOT_NAME)
        return ReminderSlot.entries.firstOrNull { it.name == name } ?: ReminderSlot.QURAN_REVISION
    }
}

class RafikReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val slot = RafikNotificationManager.resolveSlotFromIntent(intent)
        RafikNotificationManager.showReminderNotification(context, slot)
    }
}

class RafikBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            RafikNotificationManager.rescheduleAllEnabled(context)
        }
    }
}
