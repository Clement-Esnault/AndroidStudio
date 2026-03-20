package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RaidRepository {

    // Cache mémoire (remplacer par Room pour persistance entre sessions)
    private val localRaids = mutableListOf<Raid>()

    fun getLocalRaids(): List<Raid> = localRaids.toList()

    suspend fun syncFromServer(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        RaidApiService.getAllRaids().onSuccess { raids ->
            localRaids.clear()
            localRaids.addAll(raids)
        }
    }
}