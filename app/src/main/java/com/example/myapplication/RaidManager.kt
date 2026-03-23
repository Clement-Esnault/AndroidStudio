package com.example.myapplication

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.toMutableList

object RaidManager {

    // La liste "Client" qui sera affichée dans ton interface
    var listRaids = mutableListOf<Raid>()

    // Charger depuis l'API
    fun loadFromServer(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = RaidApiService.getAllRaids()
            result.onSuccess { raids ->
                listRaids = raids.toMutableList()
                onComplete(true)
            }.onFailure {
                onComplete(false)
            }
        }
    }

    // Modifier CÔTÉ CLIENT + Envoi SERVEUR
    fun updateRaid(updatedRaid: Raid) {
        // 1. Modif immédiate côté client (mémoire)
        val index = listRaids.indexOfFirst { it.id == updatedRaid.id }
        if (index != -1) {
            listRaids[index] = updatedRaid
        }

        // 2. Envoi asynchrone au serveur pour le sujet
        CoroutineScope(Dispatchers.IO).launch {
            val result = RaidApiService.updateRaid(updatedRaid)
            result.onFailure {
                // Optionnel : gérer ici si le serveur refuse (ex: remettre l'ancienne valeur)
                println("Note: Échec synchro serveur (401?), mais gardé en local.")
            }
        }
    }
}