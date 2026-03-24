package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RaidViewModel(private val repository: RaidRepository) : ViewModel() {

    private val _raids = MutableStateFlow<List<Raid>>(emptyList())
    val raids: StateFlow<List<Raid>> = _raids

    private val _isOnline = MutableStateFlow(true)

    val isOnline: StateFlow<Boolean> = _isOnline
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMsg

    // Initialisation : On charge uniquement le cache local
    init {
        loadFromCache()
    }

    fun loadFromCache() {
        viewModelScope.launch {
            _raids.value = repository.loadLocally()
        }
    }

    // BOUTON MANUEL : C'est le seul endroit qui communique avec le serveur
    fun pushToServer() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.sync().onSuccess { updatedList ->
                _raids.value = updatedList
                _errorMsg.value = null
            }.onFailure {
                _errorMsg.value = "Erreur de synchronisation : ${it.message}"
            }
            _isLoading.value = false
        }
    }

    // CREATE : Sauvegarde uniquement en local (isDirty = true)
    fun create(raid: Raid) {
        viewModelScope.launch {
            repository.create(raid)
            loadFromCache() // On rafraîchit l'UI depuis le cache local
        }
    }

    // EDIT : Sauvegarde uniquement en local (isDirty = true)
    fun edit(raid: Raid) {
        viewModelScope.launch {
            repository.update(raid)
            loadFromCache() // On rafraîchit l'UI depuis le cache local
        }
    }

    // DELETE : Suppression locale uniquement (markDeleted)
    fun delete(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            loadFromCache() // On rafraîchit l'UI depuis le cache local
        }
    }
}