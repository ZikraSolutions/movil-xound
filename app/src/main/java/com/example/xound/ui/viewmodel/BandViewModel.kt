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
                _members.value = RetrofitClient.apiService.getBandMembers(bandId)
            } catch (_: Exception) {
                _members.value = emptyList()
            }
        }
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
