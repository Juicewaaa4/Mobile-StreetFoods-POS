package com.streetfood.pos.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.data.models.UiState
import com.streetfood.pos.ui.components.*
import com.streetfood.pos.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    productViewModel: ProductViewModel,
    onBack: () -> Unit
) {
    val productsState by productViewModel.products.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    var editProduct by remember { mutableStateOf<Product?>(null) }
    var deleteProduct by remember { mutableStateOf<Product?>(null) }
    var pendingUndo by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Management", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = productsState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Empty -> EmptyStateView("", "No Products Yet", "Tap + to add your first product.", modifier = Modifier.padding(padding))
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(state.data, key = { it.id }) { product ->
                        ProductManagementRow(
                            product = product,
                            onEdit = { editProduct = product },
                            onDelete = { deleteProduct = product },
                            onToggle = { productViewModel.toggleAvailability(product) },
                            onIncreasePrice = { productViewModel.updateProduct(product.copy(price = (product.price + 1.0).coerceAtMost(999999.0))) },
                            onDecreasePrice = { productViewModel.updateProduct(product.copy(price = (product.price - 1.0).coerceAtLeast(0.0))) }
                        )
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            is UiState.Error -> EmptyStateView("⚠️", "Error", state.message, modifier = Modifier.padding(padding))
        }
    }

    // Delete confirm dialog
    deleteProduct?.let { product ->
        ConfirmDialog(
            title = "Delete \"${product.name}\"?",
            message = "This product will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = {
                pendingUndo = product
                productViewModel.deleteProduct(product)
                deleteProduct = null
            },
            onDismiss = { deleteProduct = null }
        )
    }

    // Undo snackbar
    LaunchedEffect(pendingUndo) {
        val deleted = pendingUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "\"${deleted.name}\" deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            productViewModel.addProduct(deleted.copy(id = ""))
        }
        pendingUndo = null
    }

    // Add/Edit bottom sheet
    if (showAddSheet || editProduct != null) {
        ProductFormSheet(
            existing = editProduct,
            onDismiss = { showAddSheet = false; editProduct = null },
            onSave = { product ->
                if (editProduct != null) productViewModel.updateProduct(product)
                else productViewModel.addProduct(product)
                showAddSheet = false; editProduct = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductManagementRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onIncreasePrice: () -> Unit,
    onDecreasePrice: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("Presyo: ${formatPeso(product.price)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onDecreasePrice, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Minus price")
            }
            IconButton(onClick = onIncreasePrice, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add price")
            }
        }

        Switch(
            checked = product.isAvailable,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(width = 48.dp, height = 28.dp)
        )

        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormSheet(existing: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var priceText by remember { mutableStateOf(existing?.price?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var costText by remember { mutableStateOf(existing?.cost?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var isAvailable by remember { mutableStateOf(existing?.isAvailable ?: true) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    // Auto-computed profit
    val price = priceText.toDoubleOrNull() ?: 0.0
    val cost = costText.toDoubleOrNull() ?: 0.0
    val profit = price - cost

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (existing == null) "Add Product" else "Edit Product",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Product Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50); nameError = null },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Selling Price
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it; priceError = null },
                label = { Text("Presyo / Selling Price (₱)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = priceError != null,
                supportingText = priceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Text("₱", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
            )

            // Cost / Puhunan
            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text("Puhunan / Cost (₱) — optional") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Text("₱", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )

            // Auto-computed profit display
            if (price > 0 && cost > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (profit >= 0) MaterialTheme.colorScheme.tertiaryContainer
                                         else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Kita bawat item:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profit >= 0) MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "${if (profit >= 0) "+" else ""}${formatPeso(profit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (profit >= 0) MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Available toggle
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Available for sale", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
            }

            // Save button
            Button(
                onClick = {
                    val finalPrice = priceText.toDoubleOrNull()
                    when {
                        name.isBlank() -> nameError = "Product name cannot be empty"
                        finalPrice == null || finalPrice <= 0 -> priceError = "Enter a valid selling price"
                        else -> onSave(
                            Product(
                                id = existing?.id ?: "",
                                name = name.trim(),
                                price = finalPrice,
                                cost = costText.toDoubleOrNull() ?: 0.0,
                                isAvailable = isAvailable
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Product", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
