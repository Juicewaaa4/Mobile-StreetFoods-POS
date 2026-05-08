package com.streetfood.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.*
import com.streetfood.pos.ui.components.*
import com.streetfood.pos.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    analyticsViewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val dateFilter by analyticsViewModel.dateFilter.collectAsStateWithLifecycle()
    val analyticsState by analyticsViewModel.analyticsData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Date filter chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateFilter.values().forEach { filter ->
                        FilterChip(
                            selected = dateFilter == filter,
                            onClick = { analyticsViewModel.setFilter(filter) },
                            label = { Text(filter.label) },
                            shape = RoundedCornerShape(50.dp)
                        )
                    }
                }
            }

            when (val state = analyticsState) {
                is UiState.Loading -> item { LoadingIndicator() }
                is UiState.Empty, is UiState.Success -> {
                    val data = (state as? UiState.Success)?.data ?: AnalyticsSummary()

                    // Revenue / Transactions
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Total Revenue", formatPeso(data.totalRevenue), "💰", modifier = Modifier.weight(1f))
                                StatCard("Transactions", data.transactionCount.toString(), "🧾", modifier = Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Avg. Value", formatPeso(data.avgTransactionValue), "📊", modifier = Modifier.weight(1f))
                                StatCard("Best Seller", data.bestSellerName, "⭐", modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Profit summary card (only show if cost data exists)
                    if (data.totalCost > 0 || data.totalRevenue > 0) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (data.totalProfit >= 0)
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "💹 Profit Summary (${dateFilter.label})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (data.totalProfit >= 0)
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Divider(color = if (data.totalProfit >= 0)
                                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))

                                    ProfitRow("Revenue", formatPeso(data.totalRevenue), isPositive = true,
                                        containerColor = if (data.totalProfit >= 0) MaterialTheme.colorScheme.onTertiaryContainer
                                        else MaterialTheme.colorScheme.onErrorContainer)
                                    if (data.totalCost > 0) {
                                        ProfitRow("Cost", "- ${formatPeso(data.totalCost)}", isPositive = false,
                                            containerColor = if (data.totalProfit >= 0) MaterialTheme.colorScheme.onTertiaryContainer
                                            else MaterialTheme.colorScheme.onErrorContainer)
                                        Divider(color = if (data.totalProfit >= 0)
                                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Net Profit",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (data.totalProfit >= 0) MaterialTheme.colorScheme.onTertiaryContainer
                                                else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                "${if (data.totalProfit >= 0) "+" else ""}${formatPeso(data.totalProfit)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (data.totalProfit >= 0) MaterialTheme.colorScheme.onTertiaryContainer
                                                else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    } else {
                                        Text(
                                            "Add product costs to see net profit.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (data.totalProfit >= 0) MaterialTheme.colorScheme.onTertiaryContainer.copy(0.7f)
                                            else MaterialTheme.colorScheme.onErrorContainer.copy(0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Revenue bar chart
                    if (data.dailyRevenue.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Daily Revenue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    RevenueBarChart(data = data.dailyRevenue, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }

                    // Top products table
                    if (data.topProducts.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Top Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Divider()
                                    Row(Modifier.fillMaxWidth()) {
                                        Text("#", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                                        Text("Product", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        Text("Qty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                                        Text("Revenue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                                    }
                                    Divider()
                                    data.topProducts.forEachIndexed { i, product ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${i + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), color = MaterialTheme.colorScheme.primary)
                                            Text(product.productName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                            Text(product.qtySold.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(40.dp))
                                            Text(formatPeso(product.revenue), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        if (i < data.topProducts.lastIndex) Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (data.transactionCount == 0) {
                        item { EmptyStateView("📊", "No Data", "No transactions found for ${dateFilter.label.lowercase()}.") }
                    }
                }
                is UiState.Error -> item { EmptyStateView("⚠️", "Error", state.message) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProfitRow(label: String, value: String, isPositive: Boolean, containerColor: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = containerColor.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = containerColor)
    }
}
