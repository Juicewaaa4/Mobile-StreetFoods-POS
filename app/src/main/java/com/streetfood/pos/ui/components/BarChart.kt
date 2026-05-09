package com.streetfood.pos.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Canvas-based revenue chart with visible bars on light and dark surfaces. */
@Composable
fun RevenueBarChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val selected = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val chartBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700, easing = EaseOutCubic))
    }
    val progress by animProgress.asState()

    val maxRevenue = data.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
    var tappedIndex by remember { mutableStateOf(-1) }

    Column(modifier = modifier) {
        Surface(color = chartBg) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 6.dp, vertical = 10.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val barSlotWidth = size.width.toFloat() / data.size
                            tappedIndex = (offset.x / barSlotWidth).toInt().coerceIn(0, data.size - 1)
                        }
                    }
            ) {
                val labelSpace = 34.dp.toPx()
                val chartHeight = size.height - labelSpace
                val barPad = 10.dp.toPx()
                val slotW = size.width / data.size

                repeat(4) { step ->
                    val y = chartHeight * (step + 1) / 4
                    drawLine(
                        color = outline,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                data.forEachIndexed { i, (_, value) ->
                    val rawBarH = (value / maxRevenue * chartHeight * progress).toFloat()
                    val barH = if (value > 0.0) rawBarH.coerceAtLeast(4.dp.toPx()) else 0f
                    val left = i * slotW + barPad / 2
                    val right = (i + 1) * slotW - barPad / 2
                    val width = (right - left).coerceAtLeast(3.dp.toPx())
                    val top = chartHeight - barH

                    drawRoundRect(
                        color = if (i == tappedIndex) selected else primary,
                        topLeft = Offset(left, top),
                        size = Size(width, barH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )

                    if ((i == tappedIndex || data.size <= 7) && value > 0.0 && progress > 0.9f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "₱%.0f".format(value),
                            left + width / 2,
                            (top - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
                            android.graphics.Paint().apply {
                                color = androidx.compose.ui.graphics.toArgb(onSurface)
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
