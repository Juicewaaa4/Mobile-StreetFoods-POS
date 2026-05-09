package com.streetfood.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Custom numeric keypad for direct cash input. */
@Composable
fun NumericKeypad(onKeyPress: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
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
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackspace)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isBackspace)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
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

/** Converts direct peso input to a numeric value, e.g. "125.50" -> 125.50. */
fun digitsToPeso(digits: String): Double {
    if (digits.isBlank() || digits == ".") return 0.0
    return digits.toDoubleOrNull() ?: 0.0
}

/** Formats direct peso input as a readable peso string for display in the payment screen. */
fun formatDigitsAsPeso(digits: String): String {
    val value = digitsToPeso(digits)
    return "₱%,.2f".format(value)
}
