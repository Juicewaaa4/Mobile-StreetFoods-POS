package com.streetfood.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    var cashDigits by remember { mutableStateOf("") }
    val cashValue by remember { derivedStateOf { digitsToPeso(cashDigits) } }
    val change by remember { derivedStateOf { (cashValue - totalAmount).coerceAtLeast(0.0) } }
    val isSufficient by remember { derivedStateOf { cashValue >= totalAmount } }

    var receiptData by remember { mutableStateOf<ReceiptData?>(null) }
    var pendingItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var pendingTotal by remember { mutableStateOf(0.0) }
    var pendingCash by remember { mutableStateOf(0.0) }
    var pendingChange by remember { mutableStateOf(0.0) }

    LaunchedEffect(transactionSuccess) {
        if (transactionSuccess) {
            receiptData = ReceiptData(pendingItems, pendingTotal, pendingCash, pendingChange)
            posViewModel.consumeTransactionSuccess()
        }
    }

    LaunchedEffect(paymentError) {
        val msg = paymentError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, withDismissAction = true)
        posViewModel.consumePaymentError()
    }

    BackHandler(enabled = cart.isNotEmpty() && receiptData == null) { onBack() }

    fun pay() {
        pendingItems = cart.toList()
        pendingTotal = totalAmount
        pendingCash = cashValue
        pendingChange = change
        posViewModel.processTransaction(cashDigits)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (cart.isEmpty() && receiptData == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your cart is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Back to POS")
                }
            }
        } else if (receiptData == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Order Summary ──
                item {
                    Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cart.forEach { item ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${item.quantity}× ${item.product.name}", style = MaterialTheme.typography.bodyMedium)
                                    Text(formatPeso(item.totalPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                            Divider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(formatPeso(totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // ── Cash Display ──
                item {
                    Text("Cash Received", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSufficient && cashDigits.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (cashDigits.isEmpty()) "₱0.00" else formatDigitsAsPeso(cashDigits),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (cashDigits.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                if (isSufficient) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Change:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        Text(formatPeso(change), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Text("Short by ${formatPeso(totalAmount - cashValue)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Quick Amount Buttons ──
                item {
                    Text("Quick amounts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { cashDigits = pesosToCashDigits(totalAmount) },
                            label = { Text("Exact") },
                            leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        )
                        listOf(20.0, 50.0, 100.0, 500.0).forEach { add ->
                            AssistChip(
                                onClick = {
                                    cashDigits = pesosToCashDigits((cashValue + add).coerceAtMost(999_999.99))
                                },
                                label = { Text("+${add.toInt()}") }
                            )
                        }
                    }
                }

                // ── Numpad ──
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

                // ── Complete Button ──
                item {
                    Button(
                        onClick = { pay() },
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
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ── Success Dialog ──
    receiptData?.let { data ->
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Payment Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        data.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.quantity}× ${item.product.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatPeso(item.totalPrice), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Divider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.SemiBold)
                            Text(formatPeso(data.total), fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cash", fontWeight = FontWeight.SemiBold)
                            Text(formatPeso(data.cash), fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Change", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatPeso(data.change), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                        }
                    }

                    Button(
                        onClick = {
                            receiptData = null
                            onNewTransaction()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("New Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun pesosToCashDigits(pesos: Double): String {
    val cents = (pesos * 100.0).roundToLong().coerceIn(0L, 99_999_999L)
    return cents.toString()
}

data class ReceiptData(val items: List<CartItem>, val total: Double, val cash: Double, val change: Double)
