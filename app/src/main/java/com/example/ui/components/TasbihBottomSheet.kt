package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.SunnahDuaCatalog
import com.example.model.TasbihPhrase
import com.example.ui.theme.AmiriFontFamily
import com.example.viewmodel.TasbihSessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihBottomSheet(
    tasbihState: TasbihSessionState,
    totalTasbihCount: Long,
    haptics: HapticFeedback,
    onSelectPhrase: (TasbihPhrase) -> Unit,
    onTapTasbih: () -> Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "مسبحة الذكر السريعة • الإجمالي $totalTasbihCount",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SunnahDuaCatalog.tasbihPhrases, key = { it.id }) { phrase ->
                    FilterChip(
                        selected = tasbihState.selectedPhrase.id == phrase.id,
                        onClick = { onSelectPhrase(phrase) },
                        label = { Text(phrase.text, maxLines = 1) }
                    )
                }
            }
            Text(
                text = tasbihState.selectedPhrase.text,
                fontFamily = AmiriFontFamily,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tasbihState.selectedPhrase.virtue,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val progress = if (tasbihState.targetCount > 0) {
                (tasbihState.currentCount.toFloat() / tasbihState.targetCount.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val activeColor = MaterialTheme.colorScheme.secondary

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .clickable {
                        val done = onTapTasbih()
                        haptics.performHapticFeedback(
                            if (done) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val r = (size.minDimension - stroke * 2) / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = trackColor, radius = r, center = c, style = Stroke(stroke))
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${tasbihState.currentCount}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "من ${tasbihState.targetCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
