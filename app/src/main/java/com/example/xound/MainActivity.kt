package com.example.xound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.xound.ui.screens.LoginScreen
import com.example.xound.ui.screens.RegisterScreen
import com.example.xound.ui.theme.XOUNDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XOUNDTheme {
                var currentScreen by remember { mutableStateOf("login") }
                when (currentScreen) {
                    "login" -> LoginScreen(
                        onNavigateToRegister = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        onNavigateToLogin = { currentScreen = "login" }
                    )
                }
            }
        }
    }
}
