package com.example.xound.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xound.data.model.CreateEventRequest
import com.example.xound.data.model.EventResponse
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

class EventViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<EventWithSetlistCount>>(emptyList())
    val events: StateFlow<List<EventWithSetlistCount>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _createState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val createState: StateFlow<CreateEventState> = _createState.asStateFlow()

    fun fetchEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val eventList = RetrofitClient.apiService.getEvents()
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

    fun resetCreateState() {
        _createState.value = CreateEventState.Idle
    }
}
