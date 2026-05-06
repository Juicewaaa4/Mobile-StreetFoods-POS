package com.streetfood.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streetfood.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Zoey's Street Foods",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )

                Text(
                    text = "Login to continue",
                    fontSize = 16.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        showError = false
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        showError = false
                    },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary
                    )
                )

                if (showError) {
                    Text(
                        text = "Invalid username or password",
                        color = ErrorColor,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        // Simple authentication logic
                        val isValid = when (username) {
                            "admin" -> password == "admin123"
                            "cashier" -> password == "cashier123"
                            else -> false
                        }

                        if (isValid) {
                            val role = if (username == "admin") "ADMIN" else "CASHIER"
                            onLoginSuccess(role)
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Surface
                    )
                ) {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Demo Credentials:",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Admin: admin / admin123",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Cashier: cashier / cashier123",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
