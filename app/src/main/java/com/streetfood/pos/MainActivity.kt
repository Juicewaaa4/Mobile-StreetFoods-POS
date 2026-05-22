package com.streetfood.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.navigation.AppNavigation
import com.streetfood.pos.ui.theme.StreetFoodPOSTheme
import com.streetfood.pos.util.NetworkMonitor
import com.streetfood.pos.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("rememberMe", false)
        if (!rememberMe) {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        }

        enableEdgeToEdge()
        NetworkMonitor.initialize(this)
        com.streetfood.pos.data.repository.UserSessionRepository.initialize(this)
        val firestoreSettings = com.google.firebase.firestore.firestoreSettings {
            isPersistenceEnabled = true
        }
        val firestore = FirebaseFirestore.getInstance().apply {
            this.firestoreSettings = firestoreSettings
        }

        setContent {
            StreetFoodPOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val navController = rememberNavController()

                    AppNavigation(
                        navController = navController,
                        db = firestore,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
