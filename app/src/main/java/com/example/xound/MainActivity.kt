package com.example.xound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.local.SessionManager
import com.example.xound.data.network.RetrofitClient
import kotlinx.coroutines.launch
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.screens.*
import com.example.xound.ui.theme.ThemeState
import com.example.xound.ui.theme.XOUNDTheme
import com.example.xound.ui.viewmodel.AuthViewModel
import com.example.xound.ui.viewmodel.BandViewModel
import com.example.xound.ui.viewmodel.EventViewModel
import com.example.xound.ui.viewmodel.SongViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(applicationContext)
        ThemeState.init()
        enableEdgeToEdge()
        setContent {
            XOUNDTheme {
                val authViewModel: AuthViewModel = viewModel()
                val eventViewModel: EventViewModel = viewModel()
                val songViewModel: SongViewModel = viewModel()
                val bandViewModel: BandViewModel = viewModel()
                val scope = rememberCoroutineScope()
                val initialScreen = when {
                    !SessionManager.isLoggedIn() -> "login"
                    !SessionManager.hasSelectedMode() -> "checkingBand"
                    SessionManager.isMusician() -> "musicianHome"
                    else -> "home"
                }
                var currentScreen by remember { mutableStateOf(initialScreen) }

                // Auto-logout cuando el token expira o es inválido (401/403)
                LaunchedEffect(Unit) {
                    RetrofitClient.unauthorizedEvent.collect {
                        currentScreen = "login"
                    }
                }
                var songToEdit by remember { mutableStateOf<SongResponse?>(null) }
                var songToView by remember { mutableStateOf<SongResponse?>(null) }
                var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
                var eventToEdit by remember { mutableStateOf<EventResponse?>(null) }
                var showAddToSetlist by remember { mutableStateOf(false) }
                var eventDetailOrigin by remember { mutableStateOf("events") }
                var viewSongOrigin by remember { mutableStateOf("library") }
                var viewSongEventName by remember { mutableStateOf<String?>(null) }
                var liveEvent by remember { mutableStateOf<EventResponse?>(null) }

                // Back handlers
                BackHandler(enabled = currentScreen == "register") {
                    currentScreen = "login"
                }
                BackHandler(enabled = currentScreen == "musicianHome") {
                    // Don't go back from musician home
                }
                BackHandler(enabled = currentScreen == "events" || currentScreen == "library") {
                    currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                }
                BackHandler(enabled = currentScreen == "createEvent") {
                    currentScreen = "events"
                }
                BackHandler(enabled = currentScreen == "addSong") {
                    currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
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
                BackHandler(enabled = currentScreen == "selectEventLive") {
                    currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                }
                BackHandler(enabled = currentScreen == "setlistPreview") {
                    currentScreen = "selectEventLive"
                }
                BackHandler(enabled = currentScreen == "band") {
                    currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                }
                BackHandler(enabled = currentScreen == "liveMode") {
                    currentScreen = "setlistPreview"
                }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onNavigateToRegister = { currentScreen = "register" },
                        onLoginSuccess = {
                            val role = SessionManager.getRoleName().uppercase()
                            if (role == "ADMIN" || role == "SUPER_ADMIN") {
                                SessionManager.setUserMode("admin")
                                currentScreen = "home"
                            } else if (SessionManager.hasSelectedMode()) {
                                currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                            } else {
                                currentScreen = "checkingBand"
                            }
                        },
                        authViewModel = authViewModel
                    )
                    "register" -> RegisterScreen(
                        onNavigateToLogin = { currentScreen = "login" },
                        onRegisterSuccess = { currentScreen = "roleSelection" },
                        authViewModel = authViewModel
                    )
                    "checkingBand" -> {
                        // Show nothing while checking band
                        LaunchedEffect(Unit) {
                            try {
                                val band = RetrofitClient.apiService.getMyBandAsMember()
                                if (band.id != null && band.band != "null") {
                                    SessionManager.setUserMode("musician")
                                    currentScreen = "musicianHome"
                                } else {
                                    currentScreen = "roleSelection"
                                }
                            } catch (_: Exception) {
                                currentScreen = "roleSelection"
                            }
                        }
                    }
                    "roleSelection" -> RoleSelectionScreen(
                        onSelectAdmin = { currentScreen = "home" },
                        onSelectMusician = { currentScreen = "musicianHome" },
                        onBack = {
                            authViewModel.logout()
                            currentScreen = "login"
                        }
                    )
                    "home" -> HomeScreen(
                        onLogout = {
                            authViewModel.logout()
                            currentScreen = "login"
                        },
                        onNavigateToEvents = { currentScreen = "events" },
                        onNavigateToLibrary = { currentScreen = "library" },
                        onNavigateToAddSong = { currentScreen = "addSong" },
                        onNavigateToLiveMode = { currentScreen = "selectEventLive" },
                        onNavigateToBand = { currentScreen = "band" },
                        onEventClick = { event ->
                            selectedEvent = event
                            eventDetailOrigin = "home"
                            currentScreen = "eventDetail"
                        },
                        eventViewModel = eventViewModel,
                        songViewModel = songViewModel
                    )
                    "musicianHome" -> MusicianHomeScreen(
                        onLogout = {
                            authViewModel.logout()
                            currentScreen = "login"
                        },
                        onNavigateToEvents = { currentScreen = "events" },
                        onNavigateToLibrary = { currentScreen = "library" },
                        onNavigateToLiveMode = { currentScreen = "selectEventLive" },
                        onNavigateToBand = { currentScreen = "band" },
                        onEventClick = { event ->
                            selectedEvent = event
                            eventDetailOrigin = "musicianHome"
                            currentScreen = "eventDetail"
                        },
                        onSongClick = { song ->
                            songToView = song
                            viewSongOrigin = "musicianHome"
                            viewSongEventName = null
                            currentScreen = "viewSong"
                        },
                        eventViewModel = eventViewModel,
                        songViewModel = songViewModel
                    )
                    "events" -> EventsScreen(
                        onBack = {
                            currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                        },
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
                        isAdmin = !SessionManager.isMusician(),
                        eventViewModel = eventViewModel
                    )
                    "createEvent" -> CreateEventScreen(
                        onBack = { currentScreen = "events" },
                        onPublished = { currentScreen = "events" },
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
                            isAdmin = !SessionManager.isMusician(),
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
                    "band" -> BandScreen(
                        onBack = {
                            currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                        },
                        onLeftBand = {
                            currentScreen = "roleSelection"
                        },
                        bandViewModel = bandViewModel
                    )
                    "library" -> LibraryScreen(
                        onBack = {
                            currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                        },
                        onAddSong = { currentScreen = "addSong" },
                        showAddButton = !SessionManager.isMusician(),
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
                        onBack = {
                            currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                        }
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
                    "selectEventLive" -> SelectEventScreen(
                        onBack = {
                            currentScreen = if (SessionManager.isMusician()) "musicianHome" else "home"
                        },
                        onSelectEvent = { event ->
                            liveEvent = event
                            currentScreen = "liveMode"
                        },
                        onPreviewEvent = { event ->
                            liveEvent = event
                            currentScreen = "setlistPreview"
                        },
                        eventViewModel = eventViewModel
                    )
                    "setlistPreview" -> liveEvent?.let { event ->
                        SetlistPreviewScreen(
                            event = event,
                            onBack = {
                                currentScreen = "selectEventLive"
                            },
                            onStartLive = {
                                currentScreen = "liveMode"
                            },
                            eventViewModel = eventViewModel
                        )
                    }
                    "liveMode" -> liveEvent?.let { event ->
                        LiveModeScreen(
                            event = event,
                            onBack = {
                                currentScreen = "setlistPreview"
                            },
                            eventViewModel = eventViewModel
                        )
                    }
                }
            }
        }
    }
}
