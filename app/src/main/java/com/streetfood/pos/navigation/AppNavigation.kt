package com.streetfood.pos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.UserRole
import com.streetfood.pos.data.repository.UserSessionRepository
import com.streetfood.pos.ui.screens.*
import com.streetfood.pos.util.NetworkMonitor
import com.streetfood.pos.viewmodel.*

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object CashierHome    : Screen("cashier_home")
    object AdminHome      : Screen("admin_home")
    object POS            : Screen("pos")
    object Payment        : Screen("payment")
    object Products       : Screen("products")
    object Analytics      : Screen("analytics")
    object History        : Screen("history")
    object Reports        : Screen("reports")
    object Users          : Screen("users")
    object ActivityLogs   : Screen("activity_logs")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    db: FirebaseFirestore,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current.applicationContext as android.app.Application

    // Shared ViewModel instances — preserved across navigation
    val posViewModel         : POSViewModel         = viewModel(factory = POSViewModelFactory(db))
    val productViewModel     : ProductViewModel     = viewModel(factory = ProductViewModelFactory(db))
    val transactionViewModel : TransactionViewModel = viewModel(factory = TransactionViewModelFactory(db))
    val analyticsViewModel   : AnalyticsViewModel   = viewModel(factory = AnalyticsViewModelFactory(db))
    val reportViewModel      : ReportViewModel      = viewModel(factory = ReportViewModelFactory(db))
    val userMgmtViewModel    : UserManagementViewModel = viewModel(factory = UserManagementViewModelFactory(context, db))
    val activityLogViewModel : ActivityLogViewModel = viewModel(factory = ActivityLogViewModelFactory(db))

    val isOffline by NetworkMonitor.isOffline.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (isOffline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD32F2F))
                    .padding(vertical = 4.dp, horizontal = 16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You are in offline mode. Changes will sync automatically.",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        NavHost(
            navController = navController, 
            startDestination = Screen.Login.route,
            modifier = Modifier.weight(1f)
        ) {

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { role ->
                    val dest = if (role == UserRole.ADMIN.name) Screen.AdminHome.route
                               else Screen.CashierHome.route
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
                onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.AdminHome.route) {
            AdminDashboard(
                transactionViewModel  = transactionViewModel,
                analyticsViewModel    = analyticsViewModel,
                activityLogViewModel  = activityLogViewModel,
                productViewModel      = productViewModel,
                onNavigateToPOS       = { navController.navigate(Screen.POS.route) },
                onNavigateToProducts  = { navController.navigate(Screen.Products.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToHistory   = { navController.navigate(Screen.History.route) },
                onNavigateToReports   = { navController.navigate(Screen.Reports.route) },
                onNavigateToUsers     = { navController.navigate(Screen.Users.route) },
                onNavigateToActivityLogs = { navController.navigate(Screen.ActivityLogs.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.POS.route) {
            POSScreen(
                posViewModel       = posViewModel,
                onBack             = { navController.popBackStack() },
                onProceedToPayment = { navController.navigate(Screen.Payment.route) }
            )
        }

        composable(Screen.Payment.route) {
            PaymentScreen(
                posViewModel     = posViewModel,
                onBack           = { navController.popBackStack() },
                onNewTransaction = { navController.popBackStack(Screen.POS.route, inclusive = false) }
            )
        }

        // Admin-only screens with route guard
        composable(Screen.Products.route) {
            ProductManagementScreen(productViewModel = productViewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.Analytics.route) {
            if (UserSessionRepository.isAdmin) {
                AnalyticsScreen(analyticsViewModel = analyticsViewModel, onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.History.route) {
            if (UserSessionRepository.isAdmin) {
                TransactionHistoryScreen(transactionViewModel = transactionViewModel, onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.Reports.route) {
            if (UserSessionRepository.isAdmin) {
                ReportScreen(reportViewModel = reportViewModel, onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.Users.route) {
            if (UserSessionRepository.isAdmin) {
                UserManagementScreen(viewModel = userMgmtViewModel, onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.ActivityLogs.route) {
            if (UserSessionRepository.isAdmin) {
                ActivityLogScreen(viewModel = activityLogViewModel, onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        }
    }
}
