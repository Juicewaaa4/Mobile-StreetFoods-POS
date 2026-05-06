package com.streetfood.pos.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Canvas-based bar chart.
 * @param data List of (dateLabel, revenue) pairs.
 * Bars animate in from bottom on first composition or data change.
 */
@Composable
fun RevenueBarChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Animate bars from 0→1 when data changes
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = EaseOutCubic))
    }
    val progress by animProgress.asState()

    val maxRevenue = data.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
    var tappedIndex by remember { mutableStateOf(-1) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val barSlotWidth = size.width.toFloat() / data.size
                        tappedIndex = (offset.x / barSlotWidth).toInt().coerceIn(0, data.size - 1)
                    }
                }
        ) {
            val chartHeight = size.height - 30.dp.toPx()  // reserve 30dp for labels
            val barPad = 10.dp.toPx()
            val slotW = size.width / data.size

            data.forEachIndexed { i, (_, value) ->
                val barH = (value / maxRevenue * chartHeight * progress).toFloat()
                val left = i * slotW + barPad / 2
                val right = (i + 1) * slotW - barPad / 2
                val top = chartHeight - barH

                drawRoundRect(
                    color = if (i == tappedIndex) primary else primaryContainer,
                    topLeft = Offset(left, top),
                    size = Size(right - left, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                // Tooltip: show value above tapped bar
                if (i == tappedIndex && progress > 0.9f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "₱%.0f".format(value),
                        left + (right - left) / 2,
                        (top - 6.dp.toPx()).coerceAtLeast(4.dp.toPx()),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#2E7D32")
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 11.sp.toPx()
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }

        // X-axis date labels
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
