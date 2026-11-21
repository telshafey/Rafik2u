package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Subtle repeating 8-pointed Islamic geometric star (Rub el Hizb) lattice overlay.
 */
@Composable
fun IslamicGeometricOverlay(
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    alpha: Float = 0.09f
) {
    Canvas(modifier = modifier) {
        val step = 64.dp.toPx()
        if (step <= 0f) return@Canvas
        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        val squareSide = step * 0.42f
        val stroke = Stroke(width = 1.1.dp.toPx())
        val tint = lineColor.copy(alpha = alpha)

        for (r in 0..rows) {
            for (c in 0..cols) {
                val cx = c * step + if (r % 2 == 1) step * 0.5f else 0f
                val cy = r * step
                val half = squareSide / 2f
                drawRect(
                    color = tint,
                    topLeft = Offset(cx - half, cy - half),
                    size = Size(squareSide, squareSide),
                    style = stroke
                )
                rotate(degrees = 45f, pivot = Offset(cx, cy)) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(cx - half, cy - half),
                        size = Size(squareSide, squareSide),
                        style = stroke
                    )
                }
            }
        }
    }
}

/**
 * Warm parchment Mushaf container with double ornamental hairline borders.
 */
@Composable
fun MushafParchmentFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val paperBg = if (dark) Color(0xFF13261F) else Color(0xFFFDF6E4)
    val outerGold = if (dark) Color(0xFF9F7D3B) else Color(0xFFC69439)
    val innerBorder = if (dark) Color(0xFF3E6153) else Color(0xFFE0C88F)

    Surface(
        color = paperBg,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.6.dp, outerGold.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.9.dp, innerBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                content = content
            )
        }
    }
}

private data class SparkleSpec(
    val xFraction: Float,
    val phaseOffset: Float,
    val speedScale: Float,
    val radiusDp: Float,
    val isGold: Boolean
)

/**
 * Gentle floating emerald & gold confetti/sparkle particles for achievement celebrations.
 */
@Composable
fun CelebrationParticlesOverlay(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return
    val specs = remember {
        List(18) { i ->
            SparkleSpec(
                xFraction = ((i * 37 + 13) % 94 + 3) / 100f,
                phaseOffset = (i % 6) / 6f,
                speedScale = 0.7f + (i % 4) * 0.2f,
                radiusDp = 3.0f + (i % 3) * 1.4f,
                isGold = i % 2 == 0
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "celebration_sparkles")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_tick"
    )

    val emerald = MaterialTheme.colorScheme.primary
    val gold = Color(0xFFD49A2A)

    Canvas(modifier = modifier) {
        specs.forEach { s ->
            val t = (progress * s.speedScale + s.phaseOffset) % 1f
            val y = size.height * (1f - t)
            val driftX = sin(t * 2f * PI).toFloat() * 14.dp.toPx()
            val x = size.width * s.xFraction + driftX
            val alpha = (1f - kotlin.math.abs(t - 0.5f) * 2f).coerceIn(0.05f, 0.75f)
            val baseColor = (if (s.isGold) gold else emerald).copy(alpha = alpha)

            drawCircle(
                color = baseColor,
                radius = s.radiusDp.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
