package com.streetfood.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streetfood.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userRole: String,
    onNavigateToPOS: () -> Unit,
    onNavigateToProductManagement: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Role: $userRole",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
            
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (userRole) {
                "CASHIER" -> {
                    // Cashier options
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        onClick = onNavigateToPOS
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "POS",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text(
                                    text = "Point of Sale",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Start taking orders",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                
                "ADMIN" -> {
                    // Admin options
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        onClick = onNavigateToProductManagement
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Products",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text(
                                    text = "Product Management",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Add, edit, delete products",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        onClick = onNavigateToTransactionHistory
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Transactions",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text(
                                    text = "Transaction History",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "View sales records",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        onClick = onNavigateToPOS
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "POS",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text(
                                    text = "Point of Sale",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Start taking orders",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick stats (for admin)
        if (userRole == "ADMIN") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Quick Stats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Today's Sales",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "₱0.00",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Transactions",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "0",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
