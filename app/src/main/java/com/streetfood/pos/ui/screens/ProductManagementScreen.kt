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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editProduct by remember { mutableStateOf<Product?>(null) }
    var deleteProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        productViewModel.userMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg, withDismissAction = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Products", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (com.streetfood.pos.data.repository.UserSessionRepository.isAdmin) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = productsState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Empty -> EmptyStateView(
                "", "No Products",
                "Tap + to add a product.",
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Header row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Product",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Price",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(70.dp)
                            )
                            Spacer(Modifier.width(80.dp)) // edit + delete
                        }
                        Divider()
                    }

                    items(state.data, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            isAdmin = com.streetfood.pos.data.repository.UserSessionRepository.isAdmin,
                            onEdit = { editProduct = product },
                            onDelete = { deleteProduct = product }
                        )
                        Divider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
            is UiState.Error -> EmptyStateView(
                "", "Error",
                state.message,
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Delete confirmation
    deleteProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deleteProduct = null },
            title = { Text("Delete \"${product.name}\"?", fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { productViewModel.deleteProduct(product); deleteProduct = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteProduct = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog || editProduct != null) {
        ProductFormDialog(
            existing = editProduct,
            onDismiss = { showAddDialog = false; editProduct = null },
            onSave = { product ->
                if (editProduct != null) productViewModel.updateProduct(product, oldStock = editProduct?.stock)
                else productViewModel.addProduct(product)
                showAddDialog = false; editProduct = null
            }
        )
    }
}

// ─── Product Row ─────────────────────────────────────────────────────────────

@Composable
private fun ProductRow(
    product: Product,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name + cost
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (product.isAvailable)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAdmin && product.cost > 0) {
                Text(
                    "Cost: ${formatPeso(product.cost)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Price
        Text(
            formatPeso(product.price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(70.dp)
        )

        // (Removed Stock column)

        if (isAdmin) {
            // Edit icon
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Add / Edit Dialog ───────────────────────────────────────────────────────

@Composable
private fun ProductFormDialog(
    existing: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var priceText by remember {
        mutableStateOf(
            existing?.price?.let {
                if (it == 0.0) "" else it.toBigDecimal().stripTrailingZeros().toPlainString()
            } ?: ""
        )
    }
    var costText by remember {
        mutableStateOf(
            existing?.cost?.let {
                if (it == 0.0) "" else it.toBigDecimal().stripTrailingZeros().toPlainString()
            } ?: ""
        )
    }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                if (existing == null) "Add Product" else "Edit Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50); nameError = null },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; priceError = null },
                    label = { Text("Price (₱)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Text("₱", color = MaterialTheme.colorScheme.primary) }
                )

                // Cost
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost (₱) - optional") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Text("₱", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                // ── Auto-computed profit preview ──────────────────────────
                val price = priceText.toDoubleOrNull()
                val cost  = costText.toDoubleOrNull()
                if (price != null && price > 0) {
                    val profit = if (cost != null) price - cost else null
                    val profitColor = when {
                        profit == null   -> MaterialTheme.colorScheme.onSurfaceVariant
                        profit > 0       -> Color(0xFF2E7D32)
                        else             -> MaterialTheme.colorScheme.error
                    }
                    val cardBg = when {
                        profit == null   -> MaterialTheme.colorScheme.surfaceVariant
                        profit > 0       -> Color(0xFFE8F5E9)
                        else             -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Profit per item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (profit != null) formatPeso(profit) else "Enter cost to compute",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = profitColor
                            )
                        }
                    }
                }
                // (Removed Stock Field)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPrice = priceText.toDoubleOrNull()
                    when {
                        name.isBlank()                     -> nameError = "Product name is required"
                        finalPrice == null || finalPrice <= 0 -> priceError = "Enter a valid price"
                        else -> onSave(
                            Product(
                                id = existing?.id ?: "",
                                name = name.trim(),
                                price = finalPrice,
                                cost = costText.toDoubleOrNull() ?: 0.0,
                                stock = existing?.stock ?: 0
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
