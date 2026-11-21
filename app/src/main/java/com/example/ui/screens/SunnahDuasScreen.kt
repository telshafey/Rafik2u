package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SunnahDuaCatalog
import com.example.model.CustomZikr
import com.example.model.SunnahDuaCategory
import com.example.model.SunnahDuaItem
import com.example.model.TasbihPhrase
import com.example.model.UiState
import com.example.ui.components.TasbihBottomSheet
import com.example.ui.theme.AmiriFontFamily
import com.example.viewmodel.TasbihSessionState

private enum class SunnahFilter(val labelAr: String) {
    ALL("الكل"),
    FAVORITES("المفضلة"),
    QURAN_VIRTUES("فضائل القرآن"),
    SAHIH_SUNNAH("البخاري ومسلم"),
    MEM_DUAS("أدعية التثبيت"),
    DAILY_AZKAR("الأذكار الصحيحة"),
    CUSTOM("أدعيتي الخاصة")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunnahDuasScreen(
    zikrCounters: Map<String, Int>,
    favoriteKeys: Set<String>,
    customAzkarState: UiState<List<CustomZikr>>,
    tasbihState: TasbihSessionState,
    totalTasbihCount: Long,
    fontScale: Double,
    onIncrementZikr: (String, Int) -> Unit,
    onResetZikr: (String) -> Unit,
    onToggleFavorite: (String, String, Boolean) -> Unit,
    onAddCustomZikr: (String, String, Long) -> Unit,
    onIncrementCustomZikr: (String) -> Unit,
    onDeleteCustomZikr: (String) -> Unit,
    onUpdateFontScale: (Double) -> Unit,
    onSelectTasbihPhrase: (TasbihPhrase) -> Unit,
    onTapTasbih: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var filter by remember { mutableStateOf(SunnahFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTasbihSheet by remember { mutableStateOf(false) }

    val customList = (customAzkarState as? UiState.Success)?.data ?: emptyList()

    val filteredBuiltIn = remember(filter, searchQuery, favoriteKeys) {
        if (filter == SunnahFilter.CUSTOM) emptyList()
        else SunnahDuaCatalog.items.filter { item ->
            val catMatch = when (filter) {
                SunnahFilter.ALL -> true
                SunnahFilter.FAVORITES -> item.key in favoriteKeys
                SunnahFilter.QURAN_VIRTUES -> item.category == SunnahDuaCategory.QURAN_VIRTUES
                SunnahFilter.SAHIH_SUNNAH -> item.category == SunnahDuaCategory.SAHIH_SUNNAH
                SunnahFilter.MEM_DUAS -> item.category == SunnahDuaCategory.MEMORIZATION_DUAS
                SunnahFilter.DAILY_AZKAR -> item.category == SunnahDuaCategory.DAILY_AZKAR
                SunnahFilter.CUSTOM -> false
            }
            val queryMatch = searchQuery.isBlank() ||
                item.titleAr.contains(searchQuery, ignoreCase = true) ||
                item.arabicText.contains(searchQuery, ignoreCase = true) ||
                item.benefitAr.contains(searchQuery, ignoreCase = true)
            catMatch && queryMatch
        }
    }

    val filteredCustom = remember(filter, searchQuery, customList) {
        if (filter == SunnahFilter.ALL || filter == SunnahFilter.CUSTOM) {
            customList.filter {
                searchQuery.isBlank() ||
                    it.text.contains(searchQuery, ignoreCase = true) ||
                    it.virtue.contains(searchQuery, ignoreCase = true)
            }
        } else emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("add_custom_dua_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة دعاء أو ذكر خاص")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صحيح السنة والأدعية",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "أحاديث منتقاة من صحيح البخاري ومسلم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showTasbihSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.RadioButtonChecked, null, Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("المسبحة")
                        }
                        IconButton(onClick = { onUpdateFontScale((fontScale - 0.1).coerceAtLeast(0.85)) }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                        }
                        IconButton(onClick = { onUpdateFontScale((fontScale + 0.1).coerceAtMost(1.6)) }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في الأحاديث أو الأدعية...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SunnahFilter.entries) { item ->
                        FilterChip(
                            selected = filter == item,
                            onClick = { filter = item },
                            label = { Text(item.labelAr) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredCustom.isNotEmpty()) {
                    items(filteredCustom, key = { "custom_${it.id}" }) { custom ->
                        Card(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onIncrementCustomZikr(custom.id)
                            }
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
                                    Text(
                                        text = "دعاء خاص • الهدف ${custom.targetCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    IconButton(onClick = { onDeleteCustomZikr(custom.id) }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = custom.text,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = (20.0 * fontScale).sp,
                                    lineHeight = (34.0 * fontScale).sp
                                )
                                if (custom.virtue.isNotBlank()) {
                                    Text(custom.virtue, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = "عدد المرات: ${custom.totalCompleted}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                items(filteredBuiltIn, key = { it.key }) { item ->
                    val currentCount = zikrCounters[item.key] ?: 0
                    val isFav = item.key in favoriteKeys
                    SunnahDuaCard(
                        item = item,
                        currentCount = currentCount,
                        isFavorite = isFav,
                        fontScale = fontScale,
                        onTap = {
                            if (item.repeatCount > 1) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onIncrementZikr(item.key, item.repeatCount)
                        },
                        onReset = { onResetZikr(item.key) },
                        onToggleFavorite = { onToggleFavorite(item.key, item.category.key, isFav) },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${item.titleAr}\n\n${item.arabicText}\n\n(${item.sourceAr})"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة"))
                        },
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomDuaDialog(
            onDismiss = { showAddDialog = false },
            onSave = { text, virtue, target ->
                onAddCustomZikr(text, virtue, target)
                showAddDialog = false
            }
        )
    }

    if (showTasbihSheet) {
        TasbihBottomSheet(
            tasbihState = tasbihState,
            totalTasbihCount = totalTasbihCount,
            haptics = haptics,
            onSelectPhrase = onSelectTasbihPhrase,
            onTapTasbih = onTapTasbih,
            onDismiss = { showTasbihSheet = false }
        )
    }
}

@Composable
private fun SunnahDuaCard(
    item: SunnahDuaItem,
    currentCount: Int,
    isFavorite: Boolean,
    fontScale: Double,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completed = currentCount >= item.repeatCount
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed && item.repeatCount > 1) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        onClick = onTap
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.titleAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = item.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    if (item.repeatCount > 1 && currentCount > 0) {
                        IconButton(onClick = onReset) {
                            Icon(Icons.Default.Refresh, contentDescription = "إعادة العداد")
                        }
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = item.arabicText,
                fontFamily = AmiriFontFamily,
                fontSize = (20.0 * fontScale).sp,
                lineHeight = (34.0 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${item.sourceAr} — ${item.benefitAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.repeatCount > 1) {
                LinearProgressIndicator(
                    progress = { (currentCount.toFloat() / item.repeatCount.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (completed) "تم بحمد الله" else "اضغط للعد",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (completed) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "$currentCount / ${item.repeatCount}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomDuaDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var virtue by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("33") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة دعاء أو ذكر خاص") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    label = { Text("نص الدعاء أو الذكر") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = virtue,
                    onValueChange = { if (it.length <= 300) virtue = it },
                    label = { Text("المرجع أو الفائدة (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 4) targetStr = it },
                    label = { Text("التكرار المطلوب") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toLongOrNull()?.coerceIn(1L, 10000L) ?: 33L
                    onSave(text, virtue, target)
                },
                enabled = text.trim().length >= 2
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
