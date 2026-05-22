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

    var cashInput by remember { mutableStateOf("") }
    val cashValue by remember { derivedStateOf { digitsToPeso(cashInput) } }
    val change by remember { derivedStateOf { (cashValue - totalAmount).coerceAtLeast(0.0) } }
    
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNumber by remember { mutableStateOf("") }
    

    val isSufficient by remember { derivedStateOf { cashValue >= totalAmount } }
    val canComplete by remember { 
        derivedStateOf { 
            isSufficient && (paymentMethod == "Cash" || referenceNumber.isNotBlank()) 
        } 
    }

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
        posViewModel.processTransaction(cashInput, paymentMethod, referenceNumber)
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
                            containerColor = if (isSufficient && cashInput.isNotEmpty())
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
                                text = if (cashInput.isEmpty()) "Enter cash amount" else formatDigitsAsPeso(cashInput),
                                style = if (cashInput.isEmpty()) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            if (cashInput.isNotEmpty()) {
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

                // ── Payment Method ──
                item {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentMethod == "Cash",
                            onClick = { paymentMethod = "Cash" },
                            label = { Text("Cash") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { if (paymentMethod == "Cash") Icon(Icons.Default.Check, null) }
                        )
                        FilterChip(
                            selected = paymentMethod == "GCash",
                            onClick = { paymentMethod = "GCash" },
                            label = { Text("GCash") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { if (paymentMethod == "GCash") Icon(Icons.Default.Check, null) }
                        )
                    }
                }

                // ── Quick Amount Buttons (Only for Cash) ──
                if (paymentMethod == "Cash") {
                    item {
                        Text("Quick amounts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { cashInput = pesoInputText(totalAmount) },
                                label = { Text("Exact") },
                                leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            )
                            listOf(20.0, 50.0, 100.0, 500.0).forEach { add ->
                                AssistChip(
                                    onClick = {
                                        cashInput = pesoInputText((cashValue + add).coerceAtMost(999_999.99))
                                    },
                                    label = { Text("+${add.toInt()}") }
                                )
                            }
                        }
                    }
                }

                // ── Input Fields ──
                item {
                    if (paymentMethod == "GCash") {
                        OutlinedTextField(
                            value = referenceNumber,
                            onValueChange = { 
                                val digits = it.filter { char -> char.isDigit() }
                                if (digits.length <= 13) referenceNumber = digits 
                            },
                            label = { Text("GCash Reference Number") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            isError = referenceNumber.isBlank(),
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (referenceNumber.isBlank()) {
                                        Text("Reference number is required", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                    Text("${referenceNumber.length}/13", color = if (referenceNumber.length == 13) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }
                    
                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                cashInput = newValue
                            }
                        },
                        label = { Text(if (paymentMethod == "GCash") "Amount Sent" else "Amount Received") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                    )
                }

                // ── Complete Button ──
                item {
                    Button(
                        onClick = { pay() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = canComplete && !isProcessing
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

private fun pesoInputText(pesos: Double): String = "%.2f".format(pesos)

private fun updateCashInput(current: String, key: String): String = when (key) {
    "⌫" -> current.dropLast(1)
    "." -> if (current.contains(".")) current else if (current.isBlank()) "0." else "$current."
    else -> {
        val candidate = if (current == "0") key else current + key
        val decimalPlaces = if (candidate.contains(".")) candidate.substringAfter(".").length else 0
        when {
            candidate.length > 9 -> current
            candidate.contains(".") && decimalPlaces > 2 -> current
            else -> candidate
        }
    }
}

data class ReceiptData(val items: List<CartItem>, val total: Double, val cash: Double, val change: Double)
