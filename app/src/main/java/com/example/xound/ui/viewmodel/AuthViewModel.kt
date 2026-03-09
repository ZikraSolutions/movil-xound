package com.example.xound.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xound.data.local.SessionManager
import com.example.xound.data.model.AuthRequest
import com.example.xound.data.model.RegisterRequest
import com.example.xound.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val token: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email y contraseña son requeridos")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = RetrofitClient.apiService.login(AuthRequest(email.trim(), password))
                val token = response.resolveToken()
                SessionManager.saveSession(
                    token = token,
                    userId = response.user?.id,
                    name = response.user?.name,
                    email = response.user?.email
                )
                _uiState.value = AuthUiState.Success(token)
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                _uiState.value = AuthUiState.Error("Error ${e.code()}: ${body ?: e.message()}")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun register(name: String, username: String, password: String) {
        if (name.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Todos los campos son requeridos")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(name.trim(), username.trim(), password)
                )
                val token = response.resolveToken()
                SessionManager.saveSession(
                    token = token,
                    userId = response.user?.id,
                    name = response.user?.name ?: name.trim(),
                    email = response.user?.email ?: username.trim()
                )
                _uiState.value = AuthUiState.Success(token)
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                _uiState.value = AuthUiState.Error("Error ${e.code()}: ${body ?: e.message()}")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun logout() {
        SessionManager.clearSession()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
