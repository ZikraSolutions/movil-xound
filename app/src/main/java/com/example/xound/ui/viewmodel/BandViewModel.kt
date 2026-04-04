package com.example.xound.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xound.data.local.SessionManager
import com.example.xound.data.model.BandMemberResponse
import com.example.xound.data.model.BandResponse
import com.example.xound.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BandViewModel : ViewModel() {

    private val _band = MutableStateFlow<BandResponse?>(null)
    val band: StateFlow<BandResponse?> = _band.asStateFlow()

    private val _members = MutableStateFlow<List<BandMemberResponse>>(emptyList())
    val members: StateFlow<List<BandMemberResponse>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchBand() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = if (SessionManager.isMusician()) {
                    RetrofitClient.apiService.getMyBandAsMember()
                } else {
                    RetrofitClient.apiService.getMyBand()
                }
                // If band is "null" string, there's no band
                if (response.band == "null" || response.id == null) {
                    _band.value = null
                    _members.value = emptyList()
                } else {
                    _band.value = response
                    fetchMembers(response.id)
                }
            } catch (_: Exception) {
                _band.value = null
                _members.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchMembers(bandId: Long) {
        viewModelScope.launch {
            try {
                val members = RetrofitClient.apiService.getBandMembers(bandId)
                val adminId = _band.value?.adminUserId
                val adminAlreadyInList = members.any { it.userId == adminId }
                if (!adminAlreadyInList && adminId != null) {
                    // Find admin name from band name or current user
                    val adminName = if (!SessionManager.isMusician()) {
                        SessionManager.getUserName()
                    } else {
                        "Admin"
                    }
                    val adminMember = BandMemberResponse(
                        id = null,
                        userId = adminId,
                        userName = adminName,
                        userUsername = null,
                        roleName = "ADMIN"
                    )
                    _members.value = listOf(adminMember) + members
                } else {
                    _members.value = members
                }
            } catch (_: Exception) {
                _members.value = emptyList()
            }
        }
    }

    fun createBand(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                RetrofitClient.apiService.createBand(mapOf("name" to name))
                fetchBand()
            } catch (e: Exception) {
                _error.value = "Error al crear la banda"
                _isLoading.value = false
            }
        }
    }

    private val _leftBand = MutableStateFlow(false)
    val leftBand: StateFlow<Boolean> = _leftBand.asStateFlow()

    fun leaveBand() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.leaveBand()
                SessionManager.setUserMode("")
                _leftBand.value = true
            } catch (e: Exception) {
                _error.value = "Error al salir de la banda"
            }
        }
    }

    fun resetLeftBand() {
        _leftBand.value = false
    }

    fun regenerateCode() {
        viewModelScope.launch {
            try {
                val result = RetrofitClient.apiService.regenerateInviteCode()
                val newCode = result["inviteCode"]
                if (newCode != null) {
                    _band.value = _band.value?.copy(inviteCode = newCode)
                }
            } catch (e: Exception) {
                _error.value = "Error al regenerar código"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
