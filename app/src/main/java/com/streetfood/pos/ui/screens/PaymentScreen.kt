package com.streetfood.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import kotlin.math.roundToLong

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
    val paymentError by posViewModel.paymentError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cash digits buffer (e.g. "1250" → ₱12.50)
    var cashDigits by remember { mutableStateOf("") }
    val cashValue by remember { derivedStateOf { digitsToPeso(cashDigits) } }
    val change by remember { derivedStateOf { (cashValue - totalAmount).coerceAtLeast(0.0) } }
    val isSufficient by remember { derivedStateOf { cashValue >= totalAmount } }

    var showReceipt by remember { mutableStateOf(false) }
    var showBackConfirm by remember { mutableStateOf(false) }
    var showLargeSaleConfirm by remember { mutableStateOf(false) }

    // Snapshot of completed transaction for receipt (cart/total cleared before success flag)
    var receiptData by remember { mutableStateOf<ReceiptData?>(null) }
    var pendingReceiptItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var pendingReceiptTotal by remember { mutableStateOf(0.0) }
    var pendingReceiptCash by remember { mutableStateOf(0.0) }
    var pendingReceiptChange by remember { mutableStateOf(0.0) }

    // Show success dialog when transaction completes
    LaunchedEffect(transactionSuccess) {
        if (transactionSuccess) {
            receiptData = ReceiptData(pendingReceiptItems, pendingReceiptTotal, pendingReceiptCash, pendingReceiptChange)
            posViewModel.consumeTransactionSuccess()
        }
    }

    LaunchedEffect(paymentError) {
        val msg = paymentError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        posViewModel.consumePaymentError()
    }

    BackHandler(enabled = cart.isNotEmpty()) {
        showBackConfirm = true
    }

    fun snapshotReceiptAndPay() {
        pendingReceiptItems = cart.toList()
        pendingReceiptTotal = totalAmount
        pendingReceiptCash = cashValue
        pendingReceiptChange = change
        posViewModel.processTransaction(cashDigits)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (cart.isEmpty()) onBack() else showBackConfirm = true }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (cart.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Walang laman ang cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Magdagdag muna ng produkto sa Point of Sale bago mag-bayad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Bumalik sa POS", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order summary
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(formatPeso(totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Cash display
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Cash Received", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Parang cash register: ang huling 2 digit ay sentimo. Hal. 1250 = ₱12.50.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (cashDigits.isEmpty()) "₱0.00" else formatDigitsAsPeso(cashDigits),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (cashDigits.isNotEmpty()) {
                            if (isSufficient) {
                                val animatedChange by animateFloatAsState(targetValue = change.toFloat(), animationSpec = tween(300), label = "change")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Change", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(formatPeso(animatedChange.toDouble()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text("Kulang ang bayad", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Quick cash chips
                item {
                    val scroll = rememberScrollState()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mabilis na dagdag", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { cashDigits = pesosToCashDigits(totalAmount) },
                                label = { Text("Tama sa total") },
                                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, Modifier.size(18.dp)) }
                            )
                            listOf(20.0, 50.0, 100.0, 500.0).forEach { add ->
                                AssistChip(
                                    onClick = {
                                        val next = (cashValue + add).coerceAtMost(999_999.99)
                                        cashDigits = pesosToCashDigits(next)
                                    },
                                    label = { Text("+${add.toInt()}") }
                                )
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
                                "00" -> if (cashDigits.isNotEmpty() && cashDigits.length + 2 <= 8) cashDigits += "00"
                                else -> if (cashDigits.length < 8) cashDigits += key
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Complete Transaction button
                item {
                    Button(
                        onClick = {
                            if (totalAmount >= 500.0) showLargeSaleConfirm = true
                            else snapshotReceiptAndPay()
                        },
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
    }

    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("Bumalik sa POS?", fontWeight = FontWeight.Bold) },
            text = { Text("Nandiyan pa rin ang items sa cart mo. Puwede kang magdagdag pa bago mag-bayad ulit.") },
            confirmButton = {
                TextButton(onClick = { showBackConfirm = false; onBack() }) { Text("Bumalik") }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showLargeSaleConfirm) {
        AlertDialog(
            onDismissRequest = { showLargeSaleConfirm = false },
            title = { Text("Kumpirmahin ang sale", fontWeight = FontWeight.Bold) },
            text = { Text("Total: ${formatPeso(totalAmount)}. Bayad: ${formatDigitsAsPeso(cashDigits)}. Ituloy?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLargeSaleConfirm = false
                        snapshotReceiptAndPay()
                    }
                ) { Text("Oo, ituloy", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLargeSaleConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Success dialog
    receiptData?.let { data ->
        TransactionSuccessDialog(
            data = data,
            onNewTransaction = {
                receiptData = null
                pendingReceiptItems = emptyList()
                pendingReceiptTotal = 0.0
                pendingReceiptCash = 0.0
                pendingReceiptChange = 0.0
                onNewTransaction()
            },
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
                    onClose = {
                        showReceipt = false
                        receiptData = null
                        pendingReceiptItems = emptyList()
                        pendingReceiptTotal = 0.0
                        pendingReceiptCash = 0.0
                        pendingReceiptChange = 0.0
                        onNewTransaction()
                    }
                )
            }
        }
    }
}

/** Converts a peso amount to the app's cash-register digit string (centavos as last two digits). */
private fun pesosToCashDigits(pesos: Double): String {
    val cents = (pesos * 100.0).roundToLong().coerceIn(0L, 99_999_999L)
    return cents.toString()
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
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
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
        Text("Zoey's Street Foods", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
