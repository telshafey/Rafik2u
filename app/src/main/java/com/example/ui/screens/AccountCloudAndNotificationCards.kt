package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notifications.RafikNotificationManager
import com.example.notifications.ReminderSetting
import com.example.notifications.ReminderSlot
import com.example.viewmodel.CloudSyncStatus
import java.util.Locale

@Composable
fun CloudConnectionStatusCard(
    isDevMode: Boolean,
    userEmail: String?,
    cloudSyncStatus: CloudSyncStatus,
    onVerifyCloudSync: () -> Unit,
    onSwitchToGoogleAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isDevMode) Icons.Default.CloudSync else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "حالة الحساب والمزامنة (Firebase)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = if (isDevMode) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (isDevMode) "محلي" else "سحابي",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDevMode) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (isDevMode) {
                Text(
                    text = "أنت تستخدم «وضع التطوير السريع» (محفوظ على الجهاز). للمزامنة السحابية عبر Firebase وحفظ تقدمك بين الأجهزة، سجّل الدخول بحساب Google.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSwitchToGoogleAuth,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("switch_to_google_signin_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("تسجيل الدخول بحساب Google الآن")
                }
            } else {
                Text(
                    text = "الحساب المرتبط: ${userEmail ?: "حساب Google موثّق"} • قاعدة البيانات السحابية مفعلة ومحمية بقواعد الأمان.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (cloudSyncStatus) {
                    CloudSyncStatus.Idle -> Unit
                    CloudSyncStatus.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("جاري التحقق من الاتصال المباشر بـ Firebase Firestore...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is CloudSyncStatus.Verified -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الاتصال بـ Firebase Auth و Firestore يعمل بنجاح (${cloudSyncStatus.latencyMs} مللي ثانية) ✓",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    is CloudSyncStatus.Failed -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تعذر الوصول للخادم حالياً: ${cloudSyncStatus.errorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onVerifyCloudSync,
                    enabled = cloudSyncStatus != CloudSyncStatus.Checking,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("verify_firebase_sync_btn")
                ) {
                    Icon(Icons.Default.CloudSync, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("فحص الاتصال والمزامنة مع Firebase الآن")
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsCard(
    onStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(RafikNotificationManager.loadAllSettings(context)) }
    var hasPermission by remember { mutableStateOf(RafikNotificationManager.hasNotificationPermission(context)) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            pendingAction?.invoke()
            pendingAction = null
        } else {
            pendingAction = null
            onStatusMessage("يلزم السماح بإذن الإشعارات لتصلك تنبيهات الورد والأذكار")
        }
    }

    fun runWithPermission(action: () -> Unit) {
        if (RafikNotificationManager.hasNotificationPermission(context)) {
            hasPermission = true
            action()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingAction = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    fun updateSetting(slot: ReminderSlot, enabled: Boolean, hour: Int, minute: Int) {
        RafikNotificationManager.saveAndApplySetting(context, slot, enabled, hour, minute)
        settings = RafikNotificationManager.loadAllSettings(context)
        if (enabled) {
            onStatusMessage("تم ضبط تذكير «${slot.titleAr}» يومياً الساعة ${formatArabicTime(hour, minute)}")
        } else {
            onStatusMessage("تم إيقاف تذكير «${slot.titleAr}»")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "التنبيهات والتذكيرات اليومية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = if (hasPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (hasPermission) "مُفعّل" else "يحتاج إذن",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("grant_notifications_permission_btn")
                ) {
                    Text("السماح بإشعارات التذكير")
                }
            }

            settings.forEachIndexed { idx, item ->
                if (idx > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                ReminderRow(
                    setting = item,
                    onToggle = { checked ->
                        if (checked) {
                            runWithPermission {
                                updateSetting(item.slot, true, item.hour, item.minute)
                            }
                        } else {
                            updateSetting(item.slot, false, item.hour, item.minute)
                        }
                    },
                    onPickTime = {
                        TimePickerDialog(
                            context,
                            { _, pickedHour, pickedMinute ->
                                if (item.enabled) {
                                    runWithPermission {
                                        updateSetting(item.slot, true, pickedHour, pickedMinute)
                                    }
                                } else {
                                    runWithPermission {
                                        updateSetting(item.slot, true, pickedHour, pickedMinute)
                                    }
                                }
                            },
                            item.hour,
                            item.minute,
                            false
                        ).show()
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    runWithPermission {
                        val sent = RafikNotificationManager.sendTestNotificationNow(context)
                        if (sent) {
                            onStatusMessage("تم إرسال إشعار تجريبي — تحقق من شريط الإشعارات بالأعلى")
                        } else {
                            onStatusMessage("تعذر إرسال الإشعار التجريبي؛ تأكد من تفعيل إذن الإشعارات")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_test_notification_btn")
            ) {
                Icon(Icons.Default.NotificationsActive, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("إرسال إشعار تجريبي الآن")
            }
        }
    }
}

@Composable
private fun ReminderRow(
    setting: ReminderSetting,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = setting.slot.titleAr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onPickTime)
                    .minimumInteractiveComponentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "الموعد: ${formatArabicTime(setting.hour, setting.minute)} (اضغط للتغيير)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Switch(
            checked = setting.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("reminder_switch_${setting.slot.name}")
        )
    }
}

private fun formatArabicTime(hour24: Int, minute: Int): String {
    val isPm = hour24 >= 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val period = if (isPm) "م" else "ص"
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
}
