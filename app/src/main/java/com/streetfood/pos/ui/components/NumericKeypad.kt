package com.streetfood.pos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom numeric keypad for cash input.
 * Layout: 1-2-3 / 4-5-6 / 7-8-9 / 00-0-⌫
 * Emits the pressed key label via [onKeyPress].
 */
@Composable
fun NumericKeypad(onKeyPress: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("00", "0", "⌫")
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    val isBackspace = key == "⌫"
                    Button(
                        onClick = { onKeyPress(key) },
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackspace)
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isBackspace)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = key,
                            fontSize = if (isBackspace) 20.sp else 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** Converts a raw digit-string to a formatted peso value (e.g. "1250" → "₱12.50"). */
fun digitsToPeso(digits: String): Double {
    if (digits.isEmpty()) return 0.0
    return digits.toDoubleOrNull()?.div(100.0) ?: 0.0
}

/** Formats digits as a readable peso string for display in the payment screen. */
fun formatDigitsAsPeso(digits: String): String {
    val value = digitsToPeso(digits)
    return "₱%.2f".format(value)
}
