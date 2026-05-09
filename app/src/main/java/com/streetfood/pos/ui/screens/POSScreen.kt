package com.streetfood.pos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.streetfood.pos.data.models.CartItem
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.ui.components.ConfirmDialog
import com.streetfood.pos.ui.components.EmptyStateView
import com.streetfood.pos.ui.components.formatPeso
import com.streetfood.pos.viewmodel.POSViewModel
import com.streetfood.pos.viewmodel.ProductFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    posViewModel: POSViewModel,
    onBack: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val searchQuery by posViewModel.searchQuery.collectAsStateWithLifecycle()
    val filterMode by posViewModel.filterMode.collectAsStateWithLifecycle()
    val filteredProducts by posViewModel.filteredProducts.collectAsStateWithLifecycle()
    val cart by posViewModel.cart.collectAsStateWithLifecycle()
    val total by posViewModel.totalAmount.collectAsStateWithLifecycle()
    val productsLoadError by posViewModel.productsLoadError.collectAsStateWithLifecycle()

    var showCartSheet by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showProceedConfirm by remember { mutableStateOf(false) }
    var pendingRemoveItem by remember { mutableStateOf<CartItem?>(null) }
    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filterOptions = listOf(
        ProductFilterMode.ALL to "All",
        ProductFilterMode.AVAILABLE to "Available",
        ProductFilterMode.UNAVAILABLE to "Unavailable"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point of Sale", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (cart.isNotEmpty()) showExitConfirm = true else onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (cart.isNotEmpty()) showCartSheet = true },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        BadgedBox(badge = { if (cart.isNotEmpty()) Badge { Text(cart.size.toString()) } }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (cart.isEmpty()) "Cart - empty (${formatPeso(0.0)})"
                            else "View cart - ${formatPeso(total)}",
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { showProceedConfirm = true },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pay", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            productsLoadError?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { posViewModel.retryProductsLoad() }) {
                            Text("Retry")
                        }
                        IconButton(onClick = { posViewModel.dismissProductsLoadBanner() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { posViewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { posViewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { (mode, label) ->
                    item(key = mode.name) {
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { posViewModel.setFilterMode(mode) },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Product grid
            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    "",
                    "No products here",
                    if (searchQuery.isNotBlank()) "No results for \"$searchQuery\". Try another search."
                    else "No products match this filter. Try \"All\" or ask an admin to add products."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val qty by remember(product.id) {
                            posViewModel.cart.map { cartList ->
                                cartList.find { it.product.id == product.id }?.quantity ?: 0
                            }
                        }.collectAsStateWithLifecycle(initialValue = posViewModel.cartQtyFor(product.id))
                        ProductListRow(
                            product = product,
                            qty = qty,
                            onRowTapAdd = { posViewModel.addToCart(product) },
                            onIncrease = { posViewModel.addToCart(product) },
                            onDecrease = { posViewModel.removeOneFromCart(product) }
                        )
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    // Cart bottom sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = cartSheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            CartSheetContent(
                cart = cart,
                total = total,
                onUpdateQty = { item, qty -> posViewModel.updateQuantity(item, qty) },
                onRequestRemove = { pendingRemoveItem = it },
                onClearCart = { posViewModel.clearCart(); showCartSheet = false },
                onCheckout = { showProceedConfirm = true }
            )
        }
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = "Leave POS?",
            message = "You have items in your cart. Going back will keep your cart.",
            confirmLabel = "Leave",
            onConfirm = {
                showExitConfirm = false
                onBack()
            },
            onDismiss = { showExitConfirm = false }
        )
    }

    if (showProceedConfirm) {
        ConfirmDialog(
            title = "Proceed to Payment?",
            message = "Items: ${cart.size}\nTotal: ${formatPeso(total)}",
            confirmLabel = "Proceed",
            isDestructive = false,
            onConfirm = {
                showProceedConfirm = false
                showCartSheet = false
                onProceedToPayment()
            },
            onDismiss = { showProceedConfirm = false }
        )
    }

    pendingRemoveItem?.let { item ->
        ConfirmDialog(
            title = "Remove item?",
            message = "\"${item.product.name}\" will be removed from cart.",
            confirmLabel = "Remove",
            onConfirm = {
                posViewModel.removeFromCart(item)
                pendingRemoveItem = null
            },
            onDismiss = { pendingRemoveItem = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductListRow(
    product: Product,
    qty: Int,
    onRowTapAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (product.isAvailable) Modifier.clickable(onClick = onRowTapAdd) else Modifier
                )
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (product.isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPeso(product.price),
                style = MaterialTheme.typography.bodyMedium,
                color = if (product.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!product.isAvailable) {
            Text("Unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onDecrease, enabled = qty > 0, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Minus")
            }
            Text(
                qty.toString(),
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add")
            }
        }
    }
}

@Composable
private fun CartSheetContent(
    cart: List<CartItem>,
    total: Double,
    onUpdateQty: (CartItem, Int) -> Unit,
    onRequestRemove: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showClearConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Clear All")
            }
        }

        Divider()

        // Cart items
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            cart.forEach { item ->
                CartSheetItem(
                    item = item,
                    onIncrease = { onUpdateQty(item, item.quantity + 1) },
                    onDecrease = { onUpdateQty(item, item.quantity - 1) },
                    onRemove = { onRequestRemove(item) }
                )
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        // Total + Checkout
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                val compact = maxWidth < 340.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(formatPeso(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Button(onClick = onCheckout, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Checkout", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(formatPeso(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Button(onClick = onCheckout, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(52.dp)) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Checkout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        com.streetfood.pos.ui.components.ConfirmDialog(
            title = "Clear Cart?",
            message = "All items will be removed from your cart.",
            confirmLabel = "Clear",
            onConfirm = { onClearCart(); showClearConfirm = false },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun CartSheetItem(item: CartItem, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(formatPeso(item.product.price) + " each", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Quantity controls
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(40.dp)) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text(item.quantity.toString(), modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onIncrease, modifier = Modifier.size(40.dp)) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(formatPeso(item.totalPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 72.dp), textAlign = TextAlign.End)
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}
