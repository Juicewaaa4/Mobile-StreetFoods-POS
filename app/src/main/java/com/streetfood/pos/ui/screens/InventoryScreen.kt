package com.streetfood.pos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.RawIngredient
import com.streetfood.pos.ui.components.ConfirmDialog
import com.streetfood.pos.ui.components.EmptyStateView
import com.streetfood.pos.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editIngredient by remember { mutableStateOf<RawIngredient?>(null) }
    var adjustIngredient by remember { mutableStateOf<RawIngredient?>(null) }
    var adjustType by remember { mutableStateOf("") } // "add" or "deduct"
    var deleteConfirmIngredient by remember { mutableStateOf<RawIngredient?>(null) }

    val filtered = if (searchQuery.isBlank()) {
        ingredients
    } else {
        ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raw Inventory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search ingredients...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filtered.isEmpty()) {
                EmptyStateView(
                    emoji = "📦",
                    title = "No ingredients found",
                    subtitle = if (searchQuery.isNotBlank()) "Try a different search." else "Tap + to add raw ingredients."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        IngredientCard(
                            ingredient = item,
                            isAdmin = isAdmin,
                            onAddStock = {
                                adjustIngredient = item
                                adjustType = "add"
                            },
                            onDeductStock = {
                                adjustIngredient = item
                                adjustType = "deduct"
                            },
                            onEdit = { editIngredient = item },
                            onDelete = { deleteConfirmIngredient = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || editIngredient != null) {
        IngredientDialog(
            existing = editIngredient,
            onDismiss = {
                showAddDialog = false
                editIngredient = null
            },
            onSave = { ingredient ->
                viewModel.saveIngredient(ingredient) { success ->
                    if (success) Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
                editIngredient = null
            }
        )
    }

    adjustIngredient?.let { item ->
        AdjustStockDialog(
            ingredient = item,
            type = adjustType,
            onDismiss = { adjustIngredient = null },
            onConfirm = { amount ->
                val newStock = if (adjustType == "add") item.stock + amount else item.stock - amount
                viewModel.updateStock(item, newStock.coerceAtLeast(0)) {
                    Toast.makeText(context, "Stock updated", Toast.LENGTH_SHORT).show()
                }
                adjustIngredient = null
            }
        )
    }

    deleteConfirmIngredient?.let { item ->
        ConfirmDialog(
            title = "Delete Ingredient",
            message = "Are you sure you want to remove '${item.name}'?",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteIngredient(item.id) {
                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                }
                deleteConfirmIngredient = null
            },
            onDismiss = { deleteConfirmIngredient = null }
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: RawIngredient,
    isAdmin: Boolean,
    onAddStock: () -> Unit,
    onDeductStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCritical = ingredient.stock <= ingredient.criticalLevel
    val statusColor = if (isCritical) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val statusBg = if (isCritical) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isAdmin) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusBg).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${ingredient.stock} ${ingredient.unit}",
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDeductStock, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Deduct", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onAddStock, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Restock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientDialog(
    existing: RawIngredient?,
    onDismiss: () -> Unit,
    onSave: (RawIngredient) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "pcs") }
    var stockText by remember { mutableStateOf(existing?.stock?.toString() ?: "0") }
    var criticalText by remember { mutableStateOf(existing?.criticalLevel?.toString() ?: "10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Ingredient" else "Edit Ingredient", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g. pcs, kg, packs)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) stockText = it },
                    label = { Text("Current Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = criticalText,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) criticalText = it },
                    label = { Text("Low Stock Alert Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && unit.isNotBlank()) {
                    val finalStock = stockText.toIntOrNull() ?: 0
                    val finalCritical = criticalText.toIntOrNull() ?: 10
                    onSave(
                        existing?.copy(
                            name = name.trim(),
                            unit = unit.trim(),
                            stock = finalStock,
                            criticalLevel = finalCritical
                        ) ?: RawIngredient(
                            name = name.trim(),
                            unit = unit.trim(),
                            stock = finalStock,
                            criticalLevel = finalCritical
                        )
                    )
                }
            }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AdjustStockDialog(
    ingredient: RawIngredient,
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val isAdd = type == "add"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdd) "Restock" else "Deduct Usage", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(ingredient.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Amount in ${ingredient.unit}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toIntOrNull() ?: 0
                if (amount > 0) onConfirm(amount)
            }) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
