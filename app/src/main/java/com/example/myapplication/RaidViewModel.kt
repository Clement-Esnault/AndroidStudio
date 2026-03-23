package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RaidViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RaidRepository(application.applicationContext)

    val raids        = mutableStateListOf<Raid>()
    var isLoading    by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var syncSuccess  by mutableStateOf(false)

    // Token fictif pour le mock (sera remplacé par le vrai token après login)
    private val token = "mock-token"

    // ------------------------------------------------------------------ //
    //  Synchronisation
    // ------------------------------------------------------------------ //

    fun sync() {
        viewModelScope.launch {
            isLoading    = true
            syncSuccess  = false
            errorMessage = null
            repository.syncFromServer(token)
                .onSuccess { result ->
                    raids.clear()
                    raids.addAll(result)
                    syncSuccess = true
                    launch {
                        delay(3000)
                        syncSuccess = false
                    }
                }
                .onFailure {
                    val local = repository.loadLocally()
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

    // ------------------------------------------------------------------ //
    //  Créer
    // ------------------------------------------------------------------ //

    fun create(raid: Raid) {
        viewModelScope.launch {
            repository.createRaid(raid, token)
                .onSuccess { created ->
                    raids.add(created)
                }
                .onFailure {
                    errorMessage = "Erreur création : ${it.message}"
                }
        }
    }

    // ------------------------------------------------------------------ //
    //  Modifier
    // ------------------------------------------------------------------ //

    fun edit(raid: Raid) {
        viewModelScope.launch {
            repository.updateRaid(raid, token)
                .onSuccess { updated ->
                    val index = raids.indexOfFirst { it.id == updated.id }
                    if (index != -1) raids[index] = updated
                }
                .onFailure {
                    errorMessage = "Erreur modification : ${it.message}"
                }
        }
    }

    // ------------------------------------------------------------------ //
    //  Supprimer
    // ------------------------------------------------------------------ //

    fun delete(id: Int) {
        viewModelScope.launch {
            repository.deleteRaid(id, token)
                .onSuccess {
                    raids.removeIf { it.id == id }
                }
                .onFailure {
                    errorMessage = "Erreur suppression : ${it.message}"
                }
        }
    }
}