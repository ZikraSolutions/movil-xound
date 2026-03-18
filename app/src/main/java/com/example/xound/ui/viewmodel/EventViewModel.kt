package com.example.xound.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xound.data.model.CreateEventRequest
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SetlistSongResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.data.local.SessionManager
import com.example.xound.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class EventWithSetlistCount(
    val event: EventResponse,
    val setlistCount: Int = 0
)

sealed class CreateEventState {
    object Idle : CreateEventState()
    object Loading : CreateEventState()
    data class Success(val event: EventResponse) : CreateEventState()
    data class Error(val message: String) : CreateEventState()
}

sealed class EditEventState {
    object Idle : EditEventState()
    object Loading : EditEventState()
    object Success : EditEventState()
    data class Error(val message: String) : EditEventState()
}

class EventViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<EventWithSetlistCount>>(emptyList())
    val events: StateFlow<List<EventWithSetlistCount>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _createState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val createState: StateFlow<CreateEventState> = _createState.asStateFlow()

    private val _editState = MutableStateFlow<EditEventState>(EditEventState.Idle)
    val editState: StateFlow<EditEventState> = _editState.asStateFlow()

    // Setlist for event detail
    private val _setlistSongs = MutableStateFlow<List<SetlistSongResponse>>(emptyList())
    val setlistSongs: StateFlow<List<SetlistSongResponse>> = _setlistSongs.asStateFlow()

    private val _setlistLoading = MutableStateFlow(false)
    val setlistLoading: StateFlow<Boolean> = _setlistLoading.asStateFlow()

    private val _publishDone = MutableStateFlow(false)
    val publishDone: StateFlow<Boolean> = _publishDone.asStateFlow()

    // All songs for adding to setlist
    private val _allSongs = MutableStateFlow<List<SongResponse>>(emptyList())
    val allSongs: StateFlow<List<SongResponse>> = _allSongs.asStateFlow()

    fun fetchEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val eventList = if (SessionManager.isMusician()) {
                    RetrofitClient.apiService.getPublishedEvents()
                } else {
                    RetrofitClient.apiService.getEvents()
                }
                val eventsWithCount = eventList.map { event ->
                    val count = try {
                        RetrofitClient.apiService.getSetlist(event.id).size
                    } catch (_: Exception) {
                        0
                    }
                    EventWithSetlistCount(event, count)
                }
                _events.value = eventsWithCount
            } catch (e: HttpException) {
                _error.value = "Error ${e.code()}: ${e.response()?.errorBody()?.string() ?: e.message()}"
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createEvent(title: String, eventDate: String?, venue: String?) {
        if (title.isBlank()) {
            _createState.value = CreateEventState.Error("El nombre del evento es requerido")
            return
        }
        viewModelScope.launch {
            _createState.value = CreateEventState.Loading
            try {
                val response = RetrofitClient.apiService.createEvent(
                    CreateEventRequest(title.trim(), eventDate, venue?.trim())
                )
                _createState.value = CreateEventState.Success(response)
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                _createState.value = CreateEventState.Error("Error ${e.code()}: ${body ?: e.message()}")
            } catch (e: Exception) {
                _createState.value = CreateEventState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun updateEvent(id: Long, title: String, eventDate: String?, venue: String?) {
        if (title.isBlank()) {
            _editState.value = EditEventState.Error("El nombre del evento es requerido")
            return
        }
        viewModelScope.launch {
            _editState.value = EditEventState.Loading
            try {
                RetrofitClient.apiService.updateEvent(
                    id,
                    CreateEventRequest(title.trim(), eventDate, venue?.trim())
                )
                _editState.value = EditEventState.Success
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                _editState.value = EditEventState.Error("Error ${e.code()}: ${body ?: e.message()}")
            } catch (e: Exception) {
                _editState.value = EditEventState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteEvent(eventId)
                _events.value = _events.value.filter { it.event.id != eventId }
            } catch (_: Exception) {
                _error.value = "Error al ocultar el evento"
            }
        }
    }

    fun publishEvent(id: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.togglePublish(id)
                _createState.value = CreateEventState.Idle
                fetchEvents()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al publicar"
            }
        }
    }

    fun togglePublishFromDetail(id: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.togglePublish(id)
                fetchEvents()
                _publishDone.value = true
            } catch (_: Exception) {
                _error.value = "Error al publicar"
            }
        }
    }

    fun resetPublishDone() {
        _publishDone.value = false
    }

    // Setlist management
    fun fetchSetlist(eventId: Long) {
        viewModelScope.launch {
            _setlistLoading.value = true
            try {
                val setlist = RetrofitClient.apiService.getSetlist(eventId)

                // Always fetch full songs to ensure lyrics are available
                // (the setlist JOIN may not include lyrics column on older backends)
                val songs = if (SessionManager.isMusician()) {
                    RetrofitClient.apiService.getBandSongs()
                } else {
                    RetrofitClient.apiService.getSongs()
                }
                val songMap = songs.associateBy { it.id }

                _setlistSongs.value = setlist.map { item ->
                    val fullSong = songMap[item.songId]
                    if (fullSong != null) {
                        item.copy(song = fullSong)
                    } else {
                        // Fallback to flat fields from the setlist response
                        item.copy(song = item.song ?: item.resolvedSong())
                    }
                }
            } catch (_: Exception) {
                _setlistSongs.value = emptyList()
            } finally {
                _setlistLoading.value = false
            }
        }
    }

    fun fetchAllSongs() {
        viewModelScope.launch {
            try {
                _allSongs.value = if (SessionManager.isMusician()) {
                    RetrofitClient.apiService.getBandSongs()
                } else {
                    RetrofitClient.apiService.getSongs()
                }
            } catch (_: Exception) { }
        }
    }

    fun addSongToSetlist(eventId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.addToSetlist(eventId, mapOf("songId" to songId))
                fetchSetlist(eventId)
            } catch (_: Exception) {
                _error.value = "Error al agregar canción"
            }
        }
    }

    fun removeSongFromSetlist(eventId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.removeFromSetlist(eventId, songId)
                _setlistSongs.value = _setlistSongs.value.filter { it.songId != songId }
            } catch (_: Exception) {
                _error.value = "Error al quitar canción"
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateEventState.Idle
    }

    fun resetEditState() {
        _editState.value = EditEventState.Idle
    }
}
