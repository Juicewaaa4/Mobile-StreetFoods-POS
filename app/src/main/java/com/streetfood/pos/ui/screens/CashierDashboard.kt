package com.streetfood.pos.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.Transaction
import com.streetfood.pos.data.repository.UserSessionRepository
import com.streetfood.pos.ui.components.*
import com.streetfood.pos.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierDashboard(
    transactionViewModel: TransactionViewModel,
    onNavigateToPOS: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onLogout: () -> Unit
) {
    val cashierName = UserSessionRepository.username
    val snackbarHostState = remember { SnackbarHostState() }
    val dashboardError by transactionViewModel.cashierDashboardError.collectAsStateWithLifecycle()
    val todayTransactions by transactionViewModel.getCashierTodayTransactions(cashierName)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val todaySales = todayTransactions.sumOf { it.totalAmount }
    val todayCount = todayTransactions.size

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

    LaunchedEffect(dashboardError) {
        val msg = dashboardError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        transactionViewModel.consumeCashierDashboardError()
    }

    // Greeting based on time of day
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🍢 Zoey's Street Foods", fontWeight = FontWeight.Bold) },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text(cashierName, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPOS,
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "POS") },
                    label = { Text("POS") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProducts,
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory") },
                    label = { Text("Inventory") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "My Sales") },
                    label = { Text("My Sales") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> CashierHomeTab(
                greeting = greeting,
                cashierName = cashierName,
                todaySales = todaySales,
                todayCount = todayCount,
                onStartSelling = onNavigateToPOS,
                modifier = Modifier.padding(padding)
            )
            3 -> CashierMySalesTab(
                transactions = todayTransactions,
                transactionViewModel = transactionViewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showLogoutConfirm) {
        ConfirmDialog(
            title = "Log Out?",
            message = "You will return to the sign-in screen.",
            confirmLabel = "Log Out",
            isDestructive = false,
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

@Composable
private fun CashierHomeTab(
    greeting: String,
    cashierName: String,
    todaySales: Double,
    todayCount: Int,
    onStartSelling: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Greeting card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$greeting, $cashierName! 👋", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Ready to serve? Let's go!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            // Today's stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Transactions Today",
                    value = todayCount.toString(),
                    emoji = "🧾",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Sales Today",
                    value = formatPeso(todaySales),
                    emoji = "💰",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Start selling button
            Button(
                onClick = onStartSelling,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("START SELLING", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun CashierMySalesTab(
    transactions: List<Transaction>,
    transactionViewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        EmptyStateView("🧾", "No sales yet today", "Start selling to see your transactions here.", modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Summary bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${transactions.size} transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(formatPeso(transactions.sumOf { it.totalAmount }), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions) { tx ->
                ExpandableTransactionCard(tx, transactionViewModel)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableTransactionCard(tx: Transaction, vm: TransactionViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(vm.formatTimestamp(tx.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vm.formatCurrency(tx.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${tx.items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null, modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (expanded) {
                Divider()
                tx.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.quantity}× ${item.productName}", style = MaterialTheme.typography.bodySmall)
                        Text(vm.formatCurrency(item.totalPrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
