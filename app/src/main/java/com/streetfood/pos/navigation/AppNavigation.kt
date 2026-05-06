package com.streetfood.pos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.repository.UserSessionRepository
import com.streetfood.pos.data.models.UserRole
import com.streetfood.pos.ui.screens.*
import com.streetfood.pos.viewmodel.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CashierHome : Screen("cashier_home")
    object AdminHome : Screen("admin_home")
    object POS : Screen("pos")
    object Payment : Screen("payment")
    object Products : Screen("products")
    object Analytics : Screen("analytics")
    object History : Screen("history")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    db: FirebaseFirestore,
    authViewModel: AuthViewModel
) {
    // Shared ViewModel instances (so state is preserved across nav)
    val posViewModel: POSViewModel = viewModel(factory = POSViewModelFactory(db))
    val productViewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(db))
    val transactionViewModel: TransactionViewModel = viewModel(factory = TransactionViewModelFactory(db))
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModelFactory(db))

    val startDestination = Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { role ->
                    val dest = if (role == UserRole.ADMIN.name) Screen.AdminHome.route else Screen.CashierHome.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CashierHome.route) {
            CashierDashboard(
                transactionViewModel = transactionViewModel,
                onNavigateToPOS = { navController.navigate(Screen.POS.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminHome.route) {
            AdminDashboard(
                transactionViewModel = transactionViewModel,
                analyticsViewModel = analyticsViewModel,
                onNavigateToPOS = { navController.navigate(Screen.POS.route) },
                onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.POS.route) {
            POSScreen(
                posViewModel = posViewModel,
                onBack = { navController.popBackStack() },
                onProceedToPayment = { navController.navigate(Screen.Payment.route) }
            )
        }

        composable(Screen.Payment.route) {
            PaymentScreen(
                posViewModel = posViewModel,
                onBack = { navController.popBackStack() },
                onNewTransaction = {
                    navController.popBackStack(Screen.POS.route, inclusive = false)
                }
            )
        }

        // Admin-only screens with route guard
        composable(Screen.Products.route) {
            if (UserSessionRepository.isAdmin) {
                ProductManagementScreen(
                    productViewModel = productViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(Screen.Analytics.route) {
            if (UserSessionRepository.isAdmin) {
                AnalyticsScreen(
                    analyticsViewModel = analyticsViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(Screen.History.route) {
            if (UserSessionRepository.isAdmin) {
                TransactionHistoryScreen(
                    transactionViewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
