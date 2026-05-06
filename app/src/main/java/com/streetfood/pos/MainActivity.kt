package com.streetfood.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.navigation.AppNavigation
import com.streetfood.pos.ui.theme.StreetFoodPOSTheme
import com.streetfood.pos.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database
        database = AppDatabase.getDatabase(this)

        setContent {
            StreetFoodPOSTheme {
                val authViewModel: AuthViewModel = viewModel()
                val navController = rememberNavController()

                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                val userRole by authViewModel.userRole.collectAsState()

                AppNavigation(
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    userRole = userRole,
                    database = database
                )
            }
        }
    }
}
