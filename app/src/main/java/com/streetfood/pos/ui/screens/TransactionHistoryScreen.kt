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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.data.models.Transaction
import com.streetfood.pos.ui.theme.*
import com.streetfood.pos.viewmodel.TransactionViewModel
import com.streetfood.pos.viewmodel.TransactionViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit,
    database: AppDatabase,
    transactionViewModel: TransactionViewModel = viewModel(factory = TransactionViewModelFactory(database))
) {
    val transactions by transactionViewModel.transactions.collectAsState()
    val isLoading by transactionViewModel.isLoading.collectAsState()
    val totalRevenue by transactionViewModel.totalRevenue.collectAsState()
    val totalTransactions by transactionViewModel.totalTransactions.collectAsState()

    LaunchedEffect(Unit) {
        transactionViewModel.loadTransactions()
        transactionViewModel.loadStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryGreen
                )
            }
            
            Text(
                text = "Transaction History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            IconButton(
                onClick = {
                    transactionViewModel.loadTransactions()
                    transactionViewModel.loadStats()
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PrimaryGreen
                )
            }
        }

        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Revenue",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Total Revenue",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = transactionViewModel.formatCurrency(totalRevenue),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Transactions",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Transactions",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = totalTransactions.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }
        }

        // Transactions List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryGreen,
                            strokeWidth = 3.dp
                        )
                    }
                } else if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "No Transactions",
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No transactions found",
                                fontSize = 16.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Transactions will appear here once sales are made",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(400.dp)
                    ) {
                        items(transactions) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                formatDate = { timestamp ->
                                    transactionViewModel.formatTimestamp(timestamp)
                                },
                                formatCurrency = { amount ->
                                    transactionViewModel.formatCurrency(amount)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    formatDate: (Long) -> String,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Transaction #${transaction.id}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = formatDate(transaction.timestamp),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Cashier: ${transaction.cashierName}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatCurrency(transaction.totalAmount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Cash: ${formatCurrency(transaction.cashReceived)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Change: ${formatCurrency(transaction.change)}",
                        fontSize = 12.sp,
                        color = SuccessColor
                    )
                }
            }
            
            // Items summary
            if (transaction.items.isNotEmpty()) {
                Divider(
                    color = TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Items (${transaction.items.size}):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    transaction.items.take(3).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = formatCurrency(item.totalPrice),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    if (transaction.items.size > 3) {
                        Text(
                            text = "... and ${transaction.items.size - 3} more items",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
