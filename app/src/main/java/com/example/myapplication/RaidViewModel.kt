package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RaidViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RaidRepository(application.applicationContext)

    val raids        = mutableStateListOf<Raid>()
    var isLoading    by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var syncSuccess  by mutableStateOf(false)
    var isOnline     by mutableStateOf(false)

    init {
        raids.addAll(repository.loadLocally())
        isOnline = repository.isOnline()
    }

    fun sync() {
        viewModelScope.launch {
            isLoading   = true
            syncSuccess = false
            isOnline    = repository.isOnline()
            repository.syncFromServer()
                .onSuccess { result ->
                    raids.clear()
                    raids.addAll(result)
                    syncSuccess = true
                    launch { delay(3000); syncSuccess = false }
                }
                .onFailure {
                    raids.clear()
                    raids.addAll(repository.loadLocally())
                    errorMessage = it.message
                }
            isLoading = false
        }
    }

    fun create(raid: Raid) {
        viewModelScope.launch {
            repository.createRaid(raid)
                .onSuccess { raids.add(it) }
                .onFailure { errorMessage = "Erreur création : ${it.message}" }
        }
    }

    fun edit(raid: Raid) {
        viewModelScope.launch {
            repository.updateRaid(raid)
                .onSuccess { updated ->
                    val i = raids.indexOfFirst { it.id == updated.id }
                    if (i != -1) raids[i] = updated
                }
                .onFailure { errorMessage = "Erreur modification : ${it.message}" }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            repository.deleteRaid(id)
                .onSuccess { raids.removeIf { it.id == id } }
                .onFailure { errorMessage = "Erreur suppression : ${it.message}" }
        }
    }
}