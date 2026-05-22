package com.streetfood.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.ActivityLog
import com.streetfood.pos.ui.components.EmptyStateView
import com.streetfood.pos.ui.components.LoadingIndicator
import com.streetfood.pos.viewmodel.ActivityLogState
import com.streetfood.pos.viewmodel.ActivityLogViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.streetfood.pos.data.models.DateFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    viewModel: ActivityLogViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.logsState.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()
    val roleFilter by viewModel.roleFilter.collectAsStateWithLifecycle()
    val roles = listOf("All", "Admin", "Cashier")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Date filter chips acting as calendar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilter.values().forEach { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { viewModel.setDateFilter(filter) },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            // Role filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roles.forEach { role ->
                    FilterChip(
                        selected = roleFilter == role,
                        onClick = { viewModel.setRoleFilter(role) },
                        label = { Text(role, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            when (val currState = state) {
                is ActivityLogState.Loading -> {
                    LoadingIndicator("Loading logs...")
                }
                is ActivityLogState.Error -> {
                    EmptyStateView("", "Error", currState.message)
                }
                is ActivityLogState.Success -> {
                    if (currState.logs.isEmpty()) {
                        EmptyStateView(
                            emoji = "📜",
                            title = "No Logs Yet",
                            subtitle = "Activity logs for ${dateFilter.label.lowercase()} will appear here."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currState.logs, key = { it.id }) { log ->
                                LogItemCard(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(log: ActivityLog) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault()) }
    val formattedTime = sdf.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.actionType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${log.userName} (${log.userRole})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
