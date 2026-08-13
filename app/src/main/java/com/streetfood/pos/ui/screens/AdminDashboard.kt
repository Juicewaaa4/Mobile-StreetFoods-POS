package com.streetfood.pos.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.data.models.TransactionItem
import com.streetfood.pos.data.models.UiState
import com.streetfood.pos.data.repository.UserSessionRepository
import com.streetfood.pos.ui.components.ConfirmDialog
import com.streetfood.pos.ui.components.StatCard
import com.streetfood.pos.ui.components.formatPeso
import com.streetfood.pos.viewmodel.AnalyticsViewModel
import com.streetfood.pos.viewmodel.ProductViewModel
import com.streetfood.pos.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.widget.Toast
import com.streetfood.pos.viewmodel.ActivityLogViewModel
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.RadioButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    transactionViewModel: TransactionViewModel,
    analyticsViewModel: AnalyticsViewModel,
    activityLogViewModel: ActivityLogViewModel,
    productViewModel: ProductViewModel,
    onNavigateToPOS: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToActivityLogs: () -> Unit,
    onNavigateToDailyBook: () -> Unit,
    onNavigateToMonthlySummary: () -> Unit,
    onLogout: () -> Unit
) {
    val adminName = UserSessionRepository.username
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val analyticsState by analyticsViewModel.analyticsData.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }

    val todayRevenue = (analyticsState as? UiState.Success)?.data?.totalRevenue ?: 0.0
    val todayCount = (analyticsState as? UiState.Success)?.data?.transactionCount ?: 0
    val bestSeller = (analyticsState as? UiState.Success)?.data?.bestSellerName ?: "N/A"
    val activeProducts = (analyticsState as? UiState.Success)?.data?.activeProductCount ?: 0

    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit the application?") },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("No") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arceo's Lugaw House", fontWeight = FontWeight.Bold) },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text("ADMIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) },
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$greeting, $adminName!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Here's today's overview.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Today's Revenue", formatPeso(todayRevenue), "", modifier = Modifier.weight(1f), onClick = onNavigateToAnalytics)
                        StatCard("Transactions Today", todayCount.toString(), "", modifier = Modifier.weight(1f), onClick = onNavigateToHistory)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Best Seller", bestSeller, "", modifier = Modifier.weight(1f), onClick = onNavigateToAnalytics)
                        StatCard("Active Products", activeProducts.toString(), "", modifier = Modifier.weight(1f), onClick = onNavigateToProducts)
                    }
                }
            }

            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminNavCard("POS - Start Selling", "Process orders and complete transactions", Icons.Default.ShoppingCart, onNavigateToPOS)
                    AdminNavCard("Product Management", "Add, edit, or toggle product availability", Icons.Default.Fastfood, onNavigateToProducts)
                    AdminNavCard("Raw Inventory", "Manage raw ingredients and track stock levels", Icons.Default.Inventory, onNavigateToInventory)
                    AdminNavCard("Activity Logs", "Track admin and cashier actions", Icons.Default.History, onNavigateToActivityLogs)
                    AdminNavCard("Analytics", "View sales charts and trends", Icons.Default.BarChart, onNavigateToAnalytics)
                    AdminNavCard("Transaction History", "Browse all past transactions", Icons.Default.History, onNavigateToHistory)
                    AdminNavCard("Sales Report", "Generate and download date-based sales reports", Icons.Default.Assessment, onNavigateToReports)
                    AdminNavCard("Daily Book", "Record daily expenses and track sales", Icons.Default.Book, onNavigateToDailyBook)
                    AdminNavCard("Monthly Summary", "View monthly profit and fixed expenses", Icons.Default.MonetizationOn, onNavigateToMonthlySummary)
                    AdminNavCard("User Management", "Create, edit, and delete staff accounts", Icons.Default.ManageAccounts, onNavigateToUsers)
                    AdminNavCard("Add Records", "Manually record missed sales for reports", Icons.Default.NoteAdd, onClick = { showAdjustmentDialog = true })
                    AdminNavCard("Purge Old Data", "Free up space by deleting old logs and transactions", Icons.Default.DeleteSweep, onClick = { showPurgeDialog = true })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showLogoutConfirm) {
        ConfirmDialog(
            title = "Log Out?",
            message = "You will return to the sign-in screen.",
            confirmLabel = "Log Out",
            isDestructive = false,
            onConfirm = { showLogoutConfirm = false; onLogout() },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    var showPurgeConfirmDialog by remember { mutableStateOf<Long?>(null) }

    if (showPurgeDialog) {
        PurgeDataDialog(
            onDismiss = { showPurgeDialog = false },
            onConfirm = { cutoff ->
                showPurgeConfirmDialog = cutoff
                showPurgeDialog = false
            }
        )
    }

    showPurgeConfirmDialog?.let { cutoff ->
        ConfirmDialog(
            title = "Final Confirmation",
            message = "Are you absolutely sure you want to permanently delete all data older than the selected timeframe? This action cannot be undone.",
            confirmLabel = "Yes, Delete",
            isDestructive = true,
            onConfirm = {
                showPurgeConfirmDialog = null
                var txDone = false
                var logDone = false
                transactionViewModel.purgeOldData(cutoff) { 
                    txDone = true
                    if (logDone) Toast.makeText(context, "Data purge successful", Toast.LENGTH_SHORT).show()
                }
                activityLogViewModel.purgeOldData(cutoff) { 
                    logDone = true
                    if (txDone) Toast.makeText(context, "Data purge successful", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showPurgeConfirmDialog = null }
        )
    }

    if (showAdjustmentDialog) {
        AdjustmentRecordDialog(
            productViewModel = productViewModel,
            onDismiss = { showAdjustmentDialog = false },
            onSubmit = { items, paymentMethod, refNo ->
                transactionViewModel.insertAdjustment(items, paymentMethod, refNo) { success ->
                    if (success) {
                        val totalItems = items.sumOf { it.quantity }
                        val totalAmount = items.sumOf { it.totalPrice }
                        val formatted = "₱ %,.2f".format(totalAmount)
                        activityLogViewModel.logAction("Adjustment", "Manual record: $totalItems item(s) for $formatted via $paymentMethod")
                        Toast.makeText(context, "Adjustment recorded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to record adjustment.", Toast.LENGTH_SHORT).show()
                    }
                }
                showAdjustmentDialog = false
            }
        )
    }
}

@Composable
fun PurgeDataDialog(onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val options = listOf(
        "Older than 7 Days" to 7,
        "Older than 1 Month" to 30,
        "Older than 3 Months" to 90,
        "Older than 6 Months" to 180
    )
    var selectedOption by remember { mutableStateOf(options[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Purge Old Data", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select timeframe to delete old transactions and activity logs. This cannot be undone.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                options.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        Text(option.first, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -selectedOption.second)
                    onConfirm(cal.timeInMillis)
                }
            ) { Text("Purge Data", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentRecordDialog(
    productViewModel: ProductViewModel,
    onDismiss: () -> Unit,
    onSubmit: (List<TransactionItem>, String, String?) -> Unit
) {
    val productsState by productViewModel.products.collectAsStateWithLifecycle()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()

    val quantities = remember { mutableStateMapOf<String, Int>() }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNumber by remember { mutableStateOf("") }

    val selectedItems = products.filter { (quantities[it.id] ?: 0) > 0 }
    val totalAmount = selectedItems.sumOf { it.price * (quantities[it.id] ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Records", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Record missed sales that weren't entered in POS. These will appear in your reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Payment method chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "GCash").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) },
                            shape = RoundedCornerShape(50.dp)
                        )
                    }
                }

                if (paymentMethod == "GCash") {
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Reference Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Product list
                Text("Select products & quantities:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val qty = quantities[product.id] ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (qty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(formatPeso(product.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { if (qty > 0) quantities[product.id] = qty - 1 },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(18.dp))
                                    }

                                    Text(
                                        text = qty.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    IconButton(
                                        onClick = { quantities[product.id] = qty + 1 },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Total
                if (selectedItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total (${selectedItems.sumOf { quantities[it.id] ?: 0 }} items)", fontWeight = FontWeight.Bold)
                            Text(formatPeso(totalAmount), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val items = selectedItems.map { product ->
                        val qty = quantities[product.id] ?: 0
                        TransactionItem(
                            productName = product.name,
                            quantity = qty,
                            unitPrice = product.price,
                            unitCost = product.cost,
                            totalPrice = product.price * qty
                        )
                    }
                    onSubmit(items, paymentMethod, if (paymentMethod == "GCash") referenceNumber else null)
                },
                enabled = selectedItems.isNotEmpty() && (paymentMethod != "GCash" || referenceNumber.isNotBlank())
            ) {
                Text("Submit Record", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
