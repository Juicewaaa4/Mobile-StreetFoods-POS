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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.UiState
import com.streetfood.pos.data.repository.UserSessionRepository
import com.streetfood.pos.ui.components.*
import com.streetfood.pos.viewmodel.AnalyticsViewModel
import com.streetfood.pos.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    transactionViewModel: TransactionViewModel,
    analyticsViewModel: AnalyticsViewModel,
    onNavigateToPOS: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val adminName = UserSessionRepository.username
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
        }
    }

    val analyticsState by analyticsViewModel.analyticsData.collectAsStateWithLifecycle()

    // Today's summary pulled from analytics
    val todayRevenue = (analyticsState as? UiState.Success)?.data?.totalRevenue ?: 0.0
    val todayCount = (analyticsState as? UiState.Success)?.data?.transactionCount ?: 0
    val bestSeller = (analyticsState as? UiState.Success)?.data?.bestSellerName ?: "N/A"
    val activeProducts = (analyticsState as? UiState.Success)?.data?.activeProductCount ?: 0

    val context = LocalContext.current
    BackHandler {
        (context as? Activity)?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍢 Zoey's Street Foods", fontWeight = FontWeight.Bold) },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text("ADMIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) },
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onLogout) {
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
                // Admin greeting card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$greeting, $adminName! 👋", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Here's today's overview.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            item {
                // 2×2 summary cards
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Today's Revenue", formatPeso(todayRevenue), "💰", modifier = Modifier.weight(1f), onClick = onNavigateToAnalytics)
                        StatCard("Transactions Today", todayCount.toString(), "🧾", modifier = Modifier.weight(1f), onClick = onNavigateToHistory)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Best Seller", bestSeller, "⭐", modifier = Modifier.weight(1f), onClick = onNavigateToAnalytics)
                        StatCard("Active Products", activeProducts.toString(), "🍢", modifier = Modifier.weight(1f), onClick = onNavigateToProducts)
                    }
                }
            }

            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                // Navigation action cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminNavCard("POS — Start Selling", "Process orders and complete transactions", Icons.Default.ShoppingCart, onNavigateToPOS)
                    AdminNavCard("Product Management", "Add, edit, or toggle product availability", Icons.Default.Inventory, onNavigateToProducts)
                    AdminNavCard("Analytics", "View sales reports and trends", Icons.Default.BarChart, onNavigateToAnalytics)
                    AdminNavCard("Transaction History", "Browse all past transactions", Icons.Default.History, onNavigateToHistory)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminNavCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
