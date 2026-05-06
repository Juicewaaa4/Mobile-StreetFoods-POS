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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.ui.theme.*
import com.streetfood.pos.viewmodel.ProductViewModel
import com.streetfood.pos.viewmodel.ProductViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    onBack: () -> Unit,
    database: AppDatabase,
    productViewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(database))
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val products by productViewModel.products.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val selectedProduct by productViewModel.selectedProduct.collectAsState()

    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
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
                text = "Product Management",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Surface,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Product",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Products List
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
                } else if (products.isEmpty()) {
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
                                Icons.Default.Star,
                                contentDescription = "No Products",
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No products found",
                                fontSize = 16.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Add your first product to get started",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(500.dp)
                    ) {
                        items(products) { product ->
                            ProductItem(
                                product = product,
                                onEdit = { 
                                    productViewModel.selectProduct(product)
                                    showEditDialog = true
                                },
                                onDelete = { productViewModel.deleteProduct(product) },
                                onToggleAvailability = { productViewModel.toggleProductAvailability(product) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        ProductDialog(
            product = null,
            onDismiss = { showAddDialog = false },
            onSave = { product ->
                productViewModel.addProduct(product)
                showAddDialog = false
            }
        )
    }

    // Edit Product Dialog
    if (showEditDialog) {
        selectedProduct?.let { product ->
            ProductDialog(
                product = product,
                onDismiss = { 
                    showEditDialog = false
                    productViewModel.selectProduct(null)
                },
                onSave = { updatedProduct ->
                    productViewModel.updateProduct(updatedProduct)
                    showEditDialog = false
                    productViewModel.selectProduct(null)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isAvailable) 
                Surface else TextSecondary.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (product.isAvailable) TextPrimary else TextSecondary
                )
                Text(
                    text = "₱%.2f".format(product.price),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (product.isAvailable) PrimaryGreen else TextSecondary
                )
                Text(
                    text = product.category,
                    fontSize = 14.sp,
                    color = if (product.isAvailable) TextSecondary else TextSecondary
                )
                Text(
                    text = if (product.isAvailable) "Available" else "Unavailable",
                    fontSize = 12.sp,
                    color = if (product.isAvailable) SuccessColor else ErrorColor,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleAvailability,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (product.isAvailable) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = "Toggle Availability",
                        tint = if (product.isAvailable) SuccessColor else ErrorColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Street Food") }
    var isAvailable by remember { mutableStateOf(product?.isAvailable ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Add Product" else "Edit Product",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary
                    )
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary
                    )
                )
                
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available",
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryGreen,
                            checkedTrackColor = SecondaryGreen.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val productPrice = price.toDoubleOrNull() ?: 0.0
                    val newProduct = Product(
                        id = product?.id ?: 0,
                        name = name,
                        price = productPrice,
                        category = category,
                        isAvailable = isAvailable
                    )
                    onSave(newProduct)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Surface
                ),
                enabled = name.isNotBlank() && price.isNotBlank() && price.toDoubleOrNull() != null && price.toDoubleOrNull()!! > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = null,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Surface
    )
}
