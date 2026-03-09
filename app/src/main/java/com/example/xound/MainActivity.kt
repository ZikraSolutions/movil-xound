package com.example.xound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.local.SessionManager
import com.example.xound.ui.screens.CreateEventScreen
import com.example.xound.ui.screens.EventsScreen
import com.example.xound.ui.screens.HomeScreen
import com.example.xound.ui.screens.LibraryScreen
import com.example.xound.ui.screens.LoginScreen
import com.example.xound.ui.screens.RegisterScreen
import com.example.xound.ui.theme.XOUNDTheme
import com.example.xound.ui.viewmodel.AuthViewModel
import com.example.xound.ui.viewmodel.EventViewModel
import com.example.xound.ui.viewmodel.SongViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            XOUNDTheme {
                val authViewModel: AuthViewModel = viewModel()
                val eventViewModel: EventViewModel = viewModel()
                val songViewModel: SongViewModel = viewModel()
                val initialScreen = if (SessionManager.isLoggedIn()) "home" else "login"
                var currentScreen by remember { mutableStateOf(initialScreen) }

                // Back handler
                BackHandler(enabled = currentScreen == "register") {
                    currentScreen = "login"
                }
                BackHandler(enabled = currentScreen == "events" || currentScreen == "library") {
                    currentScreen = "home"
                }
                BackHandler(enabled = currentScreen == "createEvent") {
                    currentScreen = "events"
                }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onNavigateToRegister = { currentScreen = "register" },
                        onLoginSuccess = { currentScreen = "home" },
                        authViewModel = authViewModel
                    )
                    "register" -> RegisterScreen(
                        onNavigateToLogin = { currentScreen = "login" },
                        onRegisterSuccess = { currentScreen = "login" },
                        authViewModel = authViewModel
                    )
                    "home" -> HomeScreen(
                        onLogout = {
                            authViewModel.logout()
                            currentScreen = "login"
                        },
                        onNavigateToEvents = { currentScreen = "events" },
                        onNavigateToLibrary = { currentScreen = "library" }
                    )
                    "events" -> EventsScreen(
                        onBack = { currentScreen = "home" },
                        onCreateEvent = { currentScreen = "createEvent" },
                        eventViewModel = eventViewModel
                    )
                    "createEvent" -> CreateEventScreen(
                        onBack = { currentScreen = "events" },
                        eventViewModel = eventViewModel
                    )
                    "library" -> LibraryScreen(
                        onBack = { currentScreen = "home" },
                        songViewModel = songViewModel
                    )
                }
            }
        }
    }
}
