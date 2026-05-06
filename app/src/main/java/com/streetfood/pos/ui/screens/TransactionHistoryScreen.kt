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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.*
import com.streetfood.pos.ui.components.*
import com.streetfood.pos.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryScreen(
    transactionViewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val dateFilter by transactionViewModel.dateFilter.collectAsStateWithLifecycle()
    val transactionsState by transactionViewModel.allTransactions.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by cashier name...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Date filter chips
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFilter.values().forEach { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { transactionViewModel.setDateFilter(filter) },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            when (val state = transactionsState) {
                is UiState.Loading -> LoadingIndicator()
                is UiState.Empty -> EmptyStateView("🧾", "No Transactions", "No records found for ${dateFilter.label.lowercase()}.")
                is UiState.Success -> {
                    // Filter by search + date
                    val (start, end) = dateFilter.toDateRange()
                    val filtered = state.data.filter { tx ->
                        tx.timestamp in start..end &&
                        (searchQuery.isBlank() || tx.cashierName.contains(searchQuery, ignoreCase = true))
                    }

                    if (filtered.isEmpty()) {
                        EmptyStateView("🔍", "No Results", "No transactions match your search.")
                        return@Column
                    }

                    // Summary bar
                    val totalFiltered = filtered.sumOf { it.totalAmount }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp, 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${filtered.size} transactions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(formatPeso(totalFiltered), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    // Group by date, show sticky headers
                    val grouped = filtered.groupBy { tx ->
                        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(tx.timestamp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        grouped.forEach { (dateLabel, txns) ->
                            stickyHeader {
                                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                            items(txns, key = { it.id }) { tx ->
                                ExpandableTransactionCard(tx, transactionViewModel)
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
                is UiState.Error -> EmptyStateView("⚠️", "Error", state.message)
            }
        }
    }
}
