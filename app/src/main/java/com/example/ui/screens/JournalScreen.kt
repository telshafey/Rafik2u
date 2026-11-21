package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MoodTag
import com.example.model.ReflectionEntry
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.viewmodel.CloudSyncStatus
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun JournalScreen(
    userProfileState: UiState<UserProfile>,
    reflectionsState: UiState<List<ReflectionEntry>>,
    userEmail: String?,
    isDevMode: Boolean,
    cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.Idle,
    onVerifyCloudSync: () -> Unit = {},
    onShowStatusMessage: (String) -> Unit = {},
    onUpdateRevisionGoal: (Long) -> Unit,
    onAddReflection: (String, String, MoodTag) -> Unit,
    onUpdateReflection: (String, String, String, MoodTag) -> Unit,
    onDeleteReflection: (String) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (userProfileState as? UiState.Success)?.data
    var showEditorFor by remember { mutableStateOf<ReflectionEntry?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val initialGoal = (profile?.dailyRevisionTargetPages ?: 20L).toFloat()
    var sliderGoal by remember(profile?.dailyRevisionTargetPages) { mutableFloatStateOf(initialGoal) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("add_reflection_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة ملاحظة ضبط")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "حسابي وسجل الضبط",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = profile?.displayName?.ifBlank { "رفيق القرآن" } ?: "رفيق القرآن",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (isDevMode) {
                                            Text(
                                                text = "وضع التطوير المحلي (محفوظ على الجهاز)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        } else if (!userEmail.isNullOrBlank()) {
                                            Text(
                                                text = userEmail,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = onSignOutClick,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("sign_out_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(if (isDevMode) "تبديل الحساب" else "خروج")
                                }
                            }

                            Column {
                                Text(
                                    text = "هدف المراجعة اليومي: ${sliderGoal.roundToLong()} صفحة (${(sliderGoal / 20f).let { String.format(Locale.US, "%.1f", it) }} جزء تقريباً)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Slider(
                                    value = sliderGoal,
                                    onValueChange = { sliderGoal = it },
                                    onValueChangeFinished = {
                                        onUpdateRevisionGoal(sliderGoal.roundToLong().coerceIn(1L, 80L))
                                    },
                                    valueRange = 2f..80f
                                )
                            }
                        }
                    }

                    CloudConnectionStatusCard(
                        isDevMode = isDevMode,
                        userEmail = userEmail,
                        cloudSyncStatus = cloudSyncStatus,
                        onVerifyCloudSync = onVerifyCloudSync,
                        onSwitchToGoogleAuth = onSignOutClick
                    )

                    NotificationSettingsCard(
                        onStatusMessage = onShowStatusMessage
                    )

                    Text(
                        text = "دفتر ملاحظات الضبط والمتشابهات",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            when (reflectionsState) {
                UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = reflectionsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.widthIn(max = 640.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val list = reflectionsState.data
                    if (list.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 640.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "سجّل ملاحظاتك في الضبط والتجويد",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "دوّن الآيات المتشابهة أو مواضع الوقف التي تراجعها باستمرار لتثبيت حفظك.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(list, key = { it.id }) { entry ->
                            val mood = MoodTag.fromRaw(entry.moodTag)
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 640.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(50.dp)
                                            ) {
                                                Text(
                                                    text = mood.labelAr,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = entry.dateKey,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { showEditorFor = entry },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل الملاحظة")
                                            }
                                            IconButton(
                                                onClick = { onDeleteReflection(entry.id) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف الملاحظة",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entry.body,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        HafizNoteEditorDialog(
            initialEntry = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, body, mood ->
                onAddReflection(title, body, mood)
                showCreateDialog = false
            }
        )
    }

    val editing = showEditorFor
    if (editing != null) {
        HafizNoteEditorDialog(
            initialEntry = editing,
            onDismiss = { showEditorFor = null },
            onSave = { title, body, mood ->
                onUpdateReflection(editing.id, title, body, mood)
                showEditorFor = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HafizNoteEditorDialog(
    initialEntry: ReflectionEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String, MoodTag) -> Unit
) {
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var body by remember { mutableStateOf(initialEntry?.body ?: "") }
    var selectedMood by remember {
        mutableStateOf(
            if (initialEntry != null) MoodTag.fromRaw(initialEntry.moodTag) else MoodTag.AYAH_FIX
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialEntry == null) "ملاحظة ضبط جديدة" else "تعديل الملاحظة")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("السورة / الموضع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 1500) body = it },
                    label = { Text("تفاصيل الضبط أو الرابط الذهني...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "تصنيف الملاحظة",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoodTag.entries.forEach { mood ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            label = { Text(mood.labelAr) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, body, selectedMood) },
                enabled = title.trim().isNotEmpty() && body.trim().isNotEmpty()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
