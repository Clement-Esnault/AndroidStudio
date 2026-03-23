package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RaidRepository {

    // Cache mémoire (remplacer par Room pour persistance entre sessions)
    private val localRaids = mutableListOf<Raid>()

    // ------------------------------------------------------------------ //
    //  Lecture locale
    // ------------------------------------------------------------------ //

    fun getLocalRaids(): List<Raid> = localRaids.toList()

    // ------------------------------------------------------------------ //
    //  Sync depuis le serveur
    // ------------------------------------------------------------------ //

    suspend fun syncFromServer(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        RaidApiService.getAllRaids().onSuccess { raids ->
            localRaids.clear()
            localRaids.addAll(raids)
        }
    }

    // ------------------------------------------------------------------ //
    //  Créer
    // ------------------------------------------------------------------ //

    suspend fun createRaid(raid: Raid, token: String): Result<Raid> =
        withContext(Dispatchers.IO) {
            RaidApiService.createRaid(raid, token).onSuccess { created ->
                localRaids.add(created)
            }
        }

    // ------------------------------------------------------------------ //
    //  Modifier
    // ------------------------------------------------------------------ //

    suspend fun updateRaid(raid: Raid, token: String): Result<Raid> =
        withContext(Dispatchers.IO) {
            RaidApiService.updateRaid(raid, token).onSuccess { updated ->
                val index = localRaids.indexOfFirst { it.id == updated.id }
                if (index != -1) localRaids[index] = updated
            }
        }

    // ------------------------------------------------------------------ //
    //  Supprimer
    // ------------------------------------------------------------------ //

    suspend fun deleteRaid(id: Int, token: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            RaidApiService.deleteRaid(id, token).onSuccess {
                localRaids.removeIf { it.id == id }
            }
        }
}