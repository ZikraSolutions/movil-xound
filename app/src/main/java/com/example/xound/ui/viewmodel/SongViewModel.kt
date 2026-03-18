package com.example.xound.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xound.data.local.SessionManager
import com.example.xound.data.model.SongResponse
import com.example.xound.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SongViewModel : ViewModel() {

    private val _songs = MutableStateFlow<List<SongResponse>>(emptyList())
    val songs: StateFlow<List<SongResponse>> = _songs.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun clearDeleteError() { _deleteError.value = null }

    fun fetchSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val songList = if (SessionManager.isMusician()) {
                    RetrofitClient.apiService.getBandSongs()
                } else {
                    RetrofitClient.apiService.getSongs()
                }
                _songs.value = songList
            } catch (e: HttpException) {
                _error.value = "Error ${e.code()}: ${e.response()?.errorBody()?.string() ?: e.message()}"
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchFavorites() {
        viewModelScope.launch {
            try {
                val favIds = RetrofitClient.apiService.getFavorites()
                _favorites.value = favIds.toSet()
            } catch (_: Exception) {
                // Silently fail - favorites are optional
            }
        }
    }

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            fetchSongs()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = RetrofitClient.apiService.searchSongs(query.trim())
                _songs.value = results
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al buscar"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.toggleFavorite(songId)
                val current = _favorites.value.toMutableSet()
                if (current.contains(songId)) current.remove(songId) else current.add(songId)
                _favorites.value = current
            } catch (_: Exception) {
                // Silently fail
            }
        }
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            _deleteError.value = null
            try {
                RetrofitClient.apiService.deleteSong(songId)
                _songs.value = _songs.value.filter { it.id != songId }
                _favorites.value = _favorites.value - songId
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                _deleteError.value = if (body.contains("setlist", ignoreCase = true)) {
                    "No se puede eliminar: la canción está en un setlist activo. Quítala del setlist primero."
                } else {
                    "Error al eliminar la canción"
                }
            } catch (_: Exception) {
                _deleteError.value = "Error de conexión al eliminar"
            }
        }
    }
}
