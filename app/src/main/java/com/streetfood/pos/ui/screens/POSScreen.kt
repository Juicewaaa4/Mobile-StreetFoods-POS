package com.streetfood.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.streetfood.pos.data.models.CartItem
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.ui.theme.*
import com.streetfood.pos.viewmodel.POSViewModel
import com.streetfood.pos.viewmodel.POSViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    onBack: () -> Unit,
    database: AppDatabase,
    posViewModel: POSViewModel = viewModel(factory = POSViewModelFactory(database))
) {
    var cashInput by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val cart by posViewModel.cart.collectAsState()
    val products by posViewModel.products.collectAsState()
    val totalAmount by posViewModel.totalAmount.collectAsState()
    val cashReceived by posViewModel.cashReceived.collectAsState()
    val change by posViewModel.change.collectAsState()
    val isProcessingPayment by posViewModel.isProcessingPayment.collectAsState()

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
                text = "Point of Sale",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            IconButton(
                onClick = { posViewModel.clearCart() },
                enabled = cart.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear Cart",
                    tint = if (cart.isNotEmpty()) PrimaryGreen else TextSecondary
                )
            }
        }

        // Products Section
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
                    text = "Products",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onAddToCart = { posViewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Cart Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cart",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cart is empty",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(cart) { cartItem ->
                            CartItemRow(
                                cartItem = cartItem,
                                onQuantityChange = { newQuantity ->
                                    posViewModel.updateQuantity(cartItem, newQuantity)
                                },
                                onRemove = { posViewModel.removeFromCart(cartItem) }
                            )
                        }
                    }
                    
                    // Total
                    Divider(color = TextSecondary.copy(alpha = 0.3f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "₱%.2f".format(totalAmount),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }

        // Payment Button
        Button(
            onClick = { showPaymentDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Surface
            ),
            enabled = cart.isNotEmpty()
        ) {
            Text(
                text = "Proceed to Payment",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // Payment Dialog
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Text(
                    text = "Payment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Total Amount: ₱%.2f".format(totalAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    
                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { 
                            cashInput = it
                            posViewModel.updateCashReceived(it)
                        },
                        label = { Text("Cash Received") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                    
                    if (cashInput.isNotEmpty()) {
                        Text(
                            text = "Change: ₱%.2f".format(change),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (cashReceived >= totalAmount) 
                                SuccessColor else ErrorColor
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (posViewModel.processTransaction()) {
                            showPaymentDialog = false
                            cashInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Surface
                    ),
                    enabled = cashReceived >= totalAmount && !isProcessingPayment
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Surface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Complete Transaction")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPaymentDialog = false },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SecondaryGreen.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onAddToCart
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Product",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
            
            Text(
                text = "₱%.2f".format(product.price),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = cartItem.product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "₱%.2f each".format(cartItem.product.price),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onQuantityChange(cartItem.quantity - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "-",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
            
            Text(
                text = cartItem.quantity.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.width(24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            IconButton(
                onClick = { onQuantityChange(cartItem.quantity + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
            
            Text(
                text = "₱%.2f".format(cartItem.totalPrice),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                modifier = Modifier.width(80.dp)
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = ErrorColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
