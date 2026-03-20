package com.example.myapplication

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch

class RaidViewModel : ViewModel() {

    private val repository = RaidRepository()

    val raids        = mutableStateListOf<Raid>()
    var isLoading    by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var syncSuccess  by mutableStateOf(false)

    fun sync() {
        viewModelScope.launch {
            isLoading    = true
            errorMessage = null
            repository.syncFromServer()
                .onSuccess { result ->
                    raids.clear()
                    raids.addAll(result)
                }
                .onFailure {
                    // Hors ligne : on charge le cache local
                    val local = repository.getLocalRaids()
                    raids.clear()
                    raids.addAll(local)
                    errorMessage = if (local.isEmpty())
                        "Hors ligne — aucune donnée locale disponible"
                    else
                        "Hors ligne — données locales affichées"
                }
            isLoading = false
        }
    }
}