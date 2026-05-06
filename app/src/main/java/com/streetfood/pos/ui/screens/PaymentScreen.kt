package com.streetfood.pos.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.CartItem
import com.streetfood.pos.ui.components.*
import com.streetfood.pos.viewmodel.POSViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    posViewModel: POSViewModel,
    onBack: () -> Unit,
    onNewTransaction: () -> Unit
) {
    val cart by posViewModel.cart.collectAsStateWithLifecycle()
    val totalAmount by posViewModel.totalAmount.collectAsStateWithLifecycle()
    val isProcessing by posViewModel.isProcessingPayment.collectAsStateWithLifecycle()
    val transactionSuccess by posViewModel.transactionSuccess.collectAsStateWithLifecycle()

    // Cash digits buffer (e.g. "1250" → ₱12.50)
    var cashDigits by remember { mutableStateOf("") }
    val cashValue by remember { derivedStateOf { digitsToPeso(cashDigits) } }
    val change by remember { derivedStateOf { (cashValue - totalAmount).coerceAtLeast(0.0) } }
    val isSufficient by remember { derivedStateOf { cashValue >= totalAmount } }

    var showReceipt by remember { mutableStateOf(false) }

    // Snapshot of completed transaction for receipt
    var receiptData by remember { mutableStateOf<ReceiptData?>(null) }

    // Show success dialog when transaction completes
    LaunchedEffect(transactionSuccess) {
        if (transactionSuccess) {
            receiptData = ReceiptData(cart.toList(), totalAmount, cashValue, change)
            posViewModel.consumeTransactionSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order summary
            item {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Divider()
                        cart.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.quantity}× ${item.product.name}", style = MaterialTheme.typography.bodyMedium)
                                Text(formatPeso(item.totalPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                        Divider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL DUE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text(formatPeso(totalAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Cash display
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cash Received", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = if (cashDigits.isEmpty()) "₱0.00" else formatDigitsAsPeso(cashDigits),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Change display
                        if (cashDigits.isNotEmpty()) {
                            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                            if (isSufficient) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Change:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                                    // Animated change value
                                    val animatedChange by animateFloatAsState(targetValue = change.toFloat(), animationSpec = tween(300), label = "change")
                                    Text(formatPeso(animatedChange.toDouble()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text("Insufficient amount", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Numeric keypad
            item {
                NumericKeypad(
                    onKeyPress = { key ->
                        when (key) {
                            "⌫" -> if (cashDigits.isNotEmpty()) cashDigits = cashDigits.dropLast(1)
                            "00" -> if (cashDigits.isNotEmpty()) cashDigits += "00"
                            else -> if (cashDigits.length < 8) cashDigits += key
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Complete Transaction button
            item {
                Button(
                    onClick = { posViewModel.processTransaction(cashDigits) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isSufficient && !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Complete Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Success dialog
    receiptData?.let { data ->
        TransactionSuccessDialog(
            data = data,
            onNewTransaction = { receiptData = null; onNewTransaction() },
            onViewReceipt = { showReceipt = true }
        )
    }

    // Receipt bottom sheet
    if (showReceipt) {
        receiptData?.let { data ->
            ModalBottomSheet(
                onDismissRequest = { showReceipt = false },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                ReceiptSheet(
                    data = data,
                    cashierName = com.streetfood.pos.data.repository.UserSessionRepository.username,
                    onClose = { showReceipt = false; receiptData = null; onNewTransaction() }
                )
            }
        }
    }
}

data class ReceiptData(val items: List<CartItem>, val total: Double, val cash: Double, val change: Double)

@Composable
private fun TransactionSuccessDialog(data: ReceiptData, onNewTransaction: () -> Unit, onViewReceipt: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated checkmark
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(visible = visible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("✅", fontSize = 44.sp) }
                    }
                }

                Text("Transaction Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

                // Summary
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SummaryRow("Total", formatPeso(data.total))
                        SummaryRow("Cash Given", formatPeso(data.cash))
                        SummaryRow("Change", formatPeso(data.change), highlight = true)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onViewReceipt, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Text("Receipt")
                    }
                    Button(onClick = onNewTransaction, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Text("New Sale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReceiptSheet(data: ReceiptData, cashierName: String, onClose: () -> Unit) {
    val dateStr = remember { java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🍢 Zoey's Street Foods", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("Cashier: $cashierName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Divider()
        data.items.forEach { item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.quantity}× ${item.product.name}", style = MaterialTheme.typography.bodyMedium)
                Text(formatPeso(item.totalPrice), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Divider()
        SummaryRow("Total", formatPeso(data.total))
        SummaryRow("Cash", formatPeso(data.cash))
        SummaryRow("Change", formatPeso(data.change), highlight = true)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}
