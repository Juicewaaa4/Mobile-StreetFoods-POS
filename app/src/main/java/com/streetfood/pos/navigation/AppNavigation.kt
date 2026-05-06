package com.streetfood.pos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.ui.screens.LoginScreen
import com.streetfood.pos.ui.screens.HomeScreen
import com.streetfood.pos.ui.screens.POSScreen
import com.streetfood.pos.ui.screens.ProductManagementScreen
import com.streetfood.pos.ui.screens.TransactionHistoryScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean = false,
    userRole: String? = null,
    database: AppDatabase? = null
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                userRole = userRole ?: "CASHIER",
                onNavigateToPOS = { navController.navigate("pos") },
                onNavigateToProductManagement = { navController.navigate("product_management") },
                onNavigateToTransactionHistory = { navController.navigate("transaction_history") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        
        composable("pos") {
            database?.let { db ->
                POSScreen(
                    onBack = { navController.popBackStack() },
                    database = db
                )
            }
        }
        
        composable("product_management") {
            database?.let { db ->
                ProductManagementScreen(
                    onBack = { navController.popBackStack() },
                    database = db
                )
            }
        }
        
        composable("transaction_history") {
            database?.let { db ->
                TransactionHistoryScreen(
                    onBack = { navController.popBackStack() },
                    database = db
                )
            }
        }
    }
}
