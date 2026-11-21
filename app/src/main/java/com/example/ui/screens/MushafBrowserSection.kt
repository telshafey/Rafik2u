package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranCatalog
import com.example.model.MoodTag
import com.example.model.MushafAyah
import com.example.model.SurahInfo
import com.example.ui.components.IslamicGeometricOverlay
import com.example.ui.components.MushafParchmentFrame
import com.example.ui.theme.AmiriFontFamily
import com.example.viewmodel.MushafReaderUiState

@Composable
fun MushafBrowserSection(
    readerState: MushafReaderUiState,
    lastReadSurahNumber: Int,
    readSurahsCount: Int,
    onOpenSurah: (Int) -> Unit,
    onCloseSurah: () -> Unit,
    onQuizThisSurah: (Int) -> Unit,
    onSaveAyahToJournal: (String, String, MoodTag) -> Unit,
    modifier: Modifier = Modifier
) {
    when (readerState) {
        is MushafReaderUiState.Index -> {
            MushafSurahIndex(
                lastReadSurahNumber = lastReadSurahNumber,
                readSurahsCount = readSurahsCount,
                onOpenSurah = onOpenSurah,
                modifier = modifier
            )
        }
        is MushafReaderUiState.Loading -> {
            BackHandler(onBack = onCloseSurah)
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "جاري تحميل سورة ${readerState.surahInfo.nameAr}...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onCloseSurah) {
                        Text("العودة إلى فهرس السور")
                    }
                }
            }
        }
        is MushafReaderUiState.Error -> {
            BackHandler(onBack = onCloseSurah)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = readerState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onCloseSurah) {
                            Text("العودة إلى الفهرس")
                        }
                    }
                }
            }
        }
        is MushafReaderUiState.Reading -> {
            BackHandler(onBack = onCloseSurah)
            MushafSurahReaderView(
                surahInfo = readerState.surahInfo,
                ayahs = readerState.ayahs,
                usedOfflineFallback = readerState.usedOfflineFallback,
                onBackToIndex = onCloseSurah,
                onNavigateSurah = onOpenSurah,
                onQuizThisSurah = { onQuizThisSurah(readerState.surahInfo.number) },
                onSaveAyahToJournal = onSaveAyahToJournal,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MushafSurahIndex(
    lastReadSurahNumber: Int,
    readSurahsCount: Int,
    onOpenSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedJuzFilter by remember { mutableStateOf<Int?>(null) }

    val lastReadSurah = remember(lastReadSurahNumber) {
        QuranCatalog.allSurahs.firstOrNull { it.number == lastReadSurahNumber }
    }

    val filteredSurahs = remember(searchQuery, selectedJuzFilter) {
        QuranCatalog.allSurahs.filter { surah ->
            val matchesJuz = selectedJuzFilter == null || (selectedJuzFilter in surah.juzList)
            val matchesQuery = searchQuery.isBlank() ||
                surah.nameAr.contains(searchQuery, ignoreCase = true) ||
                surah.number.toString() == searchQuery.trim()
            matchesJuz && matchesQuery
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lastReadSurah != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenSurah(lastReadSurah.number) }
                        .testTag("mushaf_continue_reading")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "متابعة القراءة: سورة ${lastReadSurah.nameAr}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${lastReadSurah.ayahCount} آية • استعرضت $readSurahsCount سور في هذه الجلسة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "فتح",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في الـ ١١٤ سورة بالاسم أو الرقم...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mushaf_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedJuzFilter == null,
                        onClick = { selectedJuzFilter = null },
                        label = { Text("الكل (١١٤)") }
                    )
                }
                items((1..30).toList()) { juz ->
                    FilterChip(
                        selected = selectedJuzFilter == juz,
                        onClick = { selectedJuzFilter = if (selectedJuzFilter == juz) null else juz },
                        label = { Text("ج$juz") }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                SurahIndexRow(
                    surah = surah,
                    onClick = { onOpenSurah(surah.number) },
                    modifier = Modifier.widthIn(max = 640.dp)
                )
            }
        }
    }
}

@Composable
private fun SurahIndexRow(
    surah: SurahInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("mushaf_surah_${surah.number}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${surah.number}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column {
                    Text(
                        text = "سورة ${surah.nameAr}",
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val firstJuz = surah.juzList.firstOrNull() ?: 1
                    val lastJuz = surah.juzList.lastOrNull() ?: firstJuz
                    val juzText = if (firstJuz == lastJuz) {
                        "الجزء $firstJuz"
                    } else {
                        "الأجزاء $firstJuz - $lastJuz"
                    }
                    Text(
                        text = "${surah.ayahCount} آية • $juzText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "فتح سورة ${surah.nameAr}",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private enum class SurahViewStyle(val labelAr: String) {
    MUSHAF_PAGE("المصحف المتصل"),
    MEMORIZE_CARDS("آية بآية للتسميع")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MushafSurahReaderView(
    surahInfo: SurahInfo,
    ayahs: List<MushafAyah>,
    usedOfflineFallback: Boolean,
    onBackToIndex: () -> Unit,
    onNavigateSurah: (Int) -> Unit,
    onQuizThisSurah: () -> Unit,
    onSaveAyahToJournal: (String, String, MoodTag) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fontScale by remember { mutableDoubleStateOf(1.1) }
    var viewStyle by remember { mutableStateOf(SurahViewStyle.MUSHAF_PAGE) }
    var hideForRecitation by remember { mutableStateOf(false) }
    var revealedAyahNums by remember(surahInfo.number) { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedAyah by remember { mutableStateOf<MushafAyah?>(null) }

    val goldAccent = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 660.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Reader Controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onBackToIndex,
                            modifier = Modifier.testTag("mushaf_back_to_index")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("الفهرس")
                        }
                        Text(
                            text = "سورة ${surahInfo.nameAr}",
                            fontFamily = AmiriFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            IconButton(
                                onClick = { fontScale = (fontScale - 0.1).coerceAtLeast(0.85) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                            }
                            IconButton(
                                onClick = { fontScale = (fontScale + 0.1).coerceAtMost(1.7) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                            }
                        }
                    }
                }

                // Reading style switcher
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SurahViewStyle.entries.forEachIndexed { idx, style ->
                        SegmentedButton(
                            selected = viewStyle == style,
                            onClick = { viewStyle = style },
                            shape = SegmentedButtonDefaults.itemShape(idx, SurahViewStyle.entries.size)
                        ) {
                            Text(style.labelAr, maxLines = 1)
                        }
                    }
                }

                if (usedOfflineFallback) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (ayahs.isEmpty()) {
                                "هذه السورة لم تُحفظ محلياً بعد. اتصل بالإنترنت لفتح نص السورة كاملاً وحفظه تلقائياً في جهازك للقراءة دون إنترنت مستقبلاً."
                            } else {
                                "وضع عدم الاتصال: تُعرض الآيات المتوفرة محلياً لهذه السورة."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Continuous Mushaf Parchment Page OR Ayah-by-Ayah Recitation Cards
                if (ayahs.isNotEmpty()) {
                    if (viewStyle == SurahViewStyle.MUSHAF_PAGE) {
                        MushafParchmentFrame {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Ornamental Surah Header Medallion
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    IslamicGeometricOverlay(
                                        modifier = Modifier.matchParentSize(),
                                        alpha = 0.12f
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "۞ سُورَةُ ${surahInfo.nameAr} ۞",
                                                fontFamily = AmiriFontFamily,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "ترتيبها ${surahInfo.number} • آياتها ${surahInfo.ayahCount} • الجزء ${surahInfo.juzList.firstOrNull() ?: 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (surahInfo.number != 1 && surahInfo.number != 9) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        fontFamily = AmiriFontFamily,
                                        fontSize = (23.0 * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    HorizontalDivider(color = goldAccent.copy(alpha = 0.35f))
                                }

                                val continuousText = remember(ayahs, goldAccent) {
                                    buildAnnotatedString {
                                        ayahs.forEachIndexed { index, ayah ->
                                            append(ayah.textUthmani)
                                            withStyle(
                                                SpanStyle(
                                                    color = goldAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append(" ﴿${ayah.numberInSurah}﴾ ")
                                            }
                                            if (index < ayahs.lastIndex) append(" ")
                                        }
                                    }
                                }

                                Text(
                                    text = continuousText,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = (23.0 * fontScale).sp,
                                    lineHeight = (44.0 * fontScale).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // Ayah-by-Ayah Memorization & Bookmark Mode
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hideForRecitation) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = if (hideForRecitation) "وضع التسميع (اضغط الآية لكشفها)" else "إخفاء الآيات للتسميع الذاتي",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                FilterChip(
                                    selected = hideForRecitation,
                                    onClick = {
                                        hideForRecitation = !hideForRecitation
                                        revealedAyahNums = emptySet()
                                    },
                                    label = { Text(if (hideForRecitation) "إظهار الكل" else "اختبر تسميعي") }
                                )
                            }
                        }

                        ayahs.forEach { ayah ->
                            val isHidden = hideForRecitation && ayah.numberInSurah !in revealedAyahNums
                            Surface(
                                color = if (isHidden) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (isHidden) {
                                            revealedAyahNums = revealedAyahNums + ayah.numberInSurah
                                        } else {
                                            selectedAyah = ayah
                                        }
                                    }
                            ) {
                                if (isHidden) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الآية ﴿${ayah.numberInSurah}﴾ — سمّع غيباً ثم اضغط للتحقق",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = "كشف الآية",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${ayah.textUthmani}  ﴿${ayah.numberInSurah}﴾",
                                        fontFamily = AmiriFontFamily,
                                        fontSize = (22.0 * fontScale).sp,
                                        lineHeight = (40.0 * fontScale).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar: Quiz + Prev / Next Surah
                Button(
                    onClick = onQuizThisSurah,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("mushaf_quiz_surah_btn")
                ) {
                    Icon(Icons.Default.Quiz, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("اختبر حفظي في سورة ${surahInfo.nameAr}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (surahInfo.number > 1) onNavigateSurah(surahInfo.number - 1) },
                        enabled = surahInfo.number > 1,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("السورة السابقة")
                    }
                    OutlinedButton(
                        onClick = { if (surahInfo.number < 114) onNavigateSurah(surahInfo.number + 1) },
                        enabled = surahInfo.number < 114,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("السورة التالية")
                    }
                }
            }
        }
    }

    val tapped = selectedAyah
    if (tapped != null) {
        AlertDialog(
            onDismissRequest = { selectedAyah = null },
            title = { Text("سورة ${tapped.surahNameAr} — الآية ${tapped.numberInSurah}") },
            text = {
                Text(
                    text = tapped.textUthmani,
                    fontFamily = AmiriFontFamily,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAyahToJournal(
                            "موضع قراءة: سورة ${tapped.surahNameAr} (${tapped.numberInSurah})",
                            tapped.textUthmani,
                            MoodTag.AYAH_FIX
                        )
                        selectedAyah = null
                    }
                ) {
                    Icon(Icons.Default.BookmarkAdd, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("تدوين بسجلي")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "﴿${tapped.textUthmani}﴾\n[سورة ${tapped.surahNameAr}: ${tapped.numberInSurah}]"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية"))
                            selectedAyah = null
                        }
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("مشاركة")
                    }
                    TextButton(onClick = { selectedAyah = null }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }
}
