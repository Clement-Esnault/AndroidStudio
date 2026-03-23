package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RaidViewModel : ViewModel() {

    private val _raids       = MutableStateFlow<List<Raid>>(emptyList())
    val raids: StateFlow<List<Raid>> = _raids

    private val _isLoading   = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMsg    = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMsg

    private val _syncSuccess = MutableStateFlow(false)
    val syncSuccess: StateFlow<Boolean> = _syncSuccess

    // ── Synchronisation des raids depuis l'API / GitHub
    fun sync() {
        viewModelScope.launch {
            _isLoading.value   = true
            _errorMsg.value    = null
            _syncSuccess.value = false

            val result = RaidApiService.getAllRaids()
            result.onSuccess { raids ->
                _raids.value       = raids
                _syncSuccess.value = true
            }.onFailure { e ->
                _errorMsg.value = e.message ?: "Erreur inconnue"
                e.printStackTrace()
            }

            _isLoading.value = false
        }
    }

    fun create(raid: Raid) {
        if (RaidApiService.SOURCE == RaidApiService.Source.GITHUB) return
        viewModelScope.launch {
            val result = RaidApiService.createRaid(raid)
            result.onSuccess { sync() }
            result.onFailure { _errorMsg.value = it.message }
        }
    }

    fun edit(raid: Raid) {
        if (RaidApiService.SOURCE == RaidApiService.Source.GITHUB) return
        viewModelScope.launch {
            val result = RaidApiService.updateRaid(raid)
            result.onSuccess { sync() }
            result.onFailure { _errorMsg.value = it.message }
        }
    }

    fun delete(id: Int) {
        if (RaidApiService.SOURCE == RaidApiService.Source.GITHUB) {
            _raids.value = _raids.value.filter { it.id != id }
            return
        }
        viewModelScope.launch {
            val result = RaidApiService.deleteRaid(id)
            result.onSuccess { sync() }
            result.onFailure { _errorMsg.value = it.message }
        }
    }
}