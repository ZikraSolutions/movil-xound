package com.example.xound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.local.SessionManager
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.screens.*
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
                var songToEdit by remember { mutableStateOf<SongResponse?>(null) }
                var songToView by remember { mutableStateOf<SongResponse?>(null) }
                var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
                var eventToEdit by remember { mutableStateOf<EventResponse?>(null) }
                var showAddToSetlist by remember { mutableStateOf(false) }
                var eventDetailOrigin by remember { mutableStateOf("events") }
                var viewSongOrigin by remember { mutableStateOf("library") }
                var viewSongEventName by remember { mutableStateOf<String?>(null) }

                // Back handlers
                BackHandler(enabled = currentScreen == "register") {
                    currentScreen = "login"
                }
                BackHandler(enabled = currentScreen == "events" || currentScreen == "library") {
                    currentScreen = "home"
                }
                BackHandler(enabled = currentScreen == "createEvent") {
                    currentScreen = "events"
                }
                BackHandler(enabled = currentScreen == "addSong") {
                    currentScreen = "home"
                }
                BackHandler(enabled = currentScreen == "editSong") {
                    currentScreen = "library"
                }
                BackHandler(enabled = currentScreen == "viewSong") {
                    currentScreen = viewSongOrigin
                }
                BackHandler(enabled = currentScreen == "eventDetail") {
                    currentScreen = eventDetailOrigin
                }
                BackHandler(enabled = currentScreen == "editEvent") {
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
                        onNavigateToLibrary = { currentScreen = "library" },
                        onNavigateToAddSong = { currentScreen = "addSong" },
                        onEventClick = { event ->
                            selectedEvent = event
                            eventDetailOrigin = "home"
                            currentScreen = "eventDetail"
                        },
                        eventViewModel = eventViewModel,
                        songViewModel = songViewModel
                    )
                    "events" -> EventsScreen(
                        onBack = { currentScreen = "home" },
                        onCreateEvent = { currentScreen = "createEvent" },
                        onEventClick = { event ->
                            selectedEvent = event
                            eventDetailOrigin = "events"
                            currentScreen = "eventDetail"
                        },
                        onEditEvent = { event ->
                            eventToEdit = event
                            currentScreen = "editEvent"
                        },
                        eventViewModel = eventViewModel
                    )
                    "createEvent" -> CreateEventScreen(
                        onBack = { currentScreen = "events" },
                        eventViewModel = eventViewModel
                    )
                    "eventDetail" -> selectedEvent?.let { event ->
                        // Add to setlist dialog
                        if (showAddToSetlist) {
                            val allSongs by eventViewModel.allSongs.collectAsState()
                            val setlistSongs by eventViewModel.setlistSongs.collectAsState()
                            val setlistSongIds = setlistSongs.map { it.songId }.toSet()

                            AddSongToSetlistDialog(
                                songs = allSongs,
                                setlistSongIds = setlistSongIds,
                                onAdd = { songId ->
                                    eventViewModel.addSongToSetlist(event.id, songId)
                                },
                                onDismiss = { showAddToSetlist = false }
                            )
                        }

                        EventDetailScreen(
                            event = event,
                            onBack = {
                                currentScreen = eventDetailOrigin
                                selectedEvent = null
                            },
                            onAddSongToSetlist = {
                                eventViewModel.fetchAllSongs()
                                showAddToSetlist = true
                            },
                            onViewSong = { song ->
                                songToView = song
                                viewSongOrigin = "eventDetail"
                                viewSongEventName = event.title
                                currentScreen = "viewSong"
                            },
                            eventViewModel = eventViewModel
                        )
                    }
                    "editEvent" -> eventToEdit?.let { event ->
                        EditEventScreen(
                            event = event,
                            onBack = {
                                currentScreen = "events"
                                eventToEdit = null
                            },
                            eventViewModel = eventViewModel
                        )
                    }
                    "library" -> LibraryScreen(
                        onBack = { currentScreen = "home" },
                        onAddSong = { currentScreen = "addSong" },
                        onEditSong = { song ->
                            songToEdit = song
                            currentScreen = "editSong"
                        },
                        onViewSong = { song ->
                            songToView = song
                            viewSongOrigin = "library"
                            viewSongEventName = null
                            currentScreen = "viewSong"
                        },
                        songViewModel = songViewModel
                    )
                    "addSong" -> AddSongScreen(
                        onBack = { currentScreen = "home" }
                    )
                    "viewSong" -> songToView?.let { song ->
                        ViewSongScreen(
                            song = song,
                            eventName = viewSongEventName,
                            onBack = {
                                currentScreen = viewSongOrigin
                                songToView = null
                            }
                        )
                    }
                    "editSong" -> songToEdit?.let { song ->
                        EditSongScreen(
                            song = song,
                            onBack = {
                                currentScreen = "library"
                                songToEdit = null
                            }
                        )
                    }
                }
            }
        }
    }
}
