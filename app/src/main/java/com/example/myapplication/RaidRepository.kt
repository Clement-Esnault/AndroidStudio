package com.example.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class RaidRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("raids_prefs", Context.MODE_PRIVATE)
    private val KEY_RAIDS = "raids_json"
    private val KEY_DIRTY = "dirty_ids"

    // ------------------------------------------------------------------ //
    //  Réseau
    // ------------------------------------------------------------------ //

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ------------------------------------------------------------------ //
    //  Persistance locale
    // ------------------------------------------------------------------ //

    private fun saveLocally(raids: List<Raid>) {
        val array = JSONArray()
        raids.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_RAIDS, array.toString()).apply()
    }

    fun loadLocally(): List<Raid> {
        val str = prefs.getString(KEY_RAIDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(str)
            List(array.length()) { Raid.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------ //
    //  Dirty IDs
    // ------------------------------------------------------------------ //

    private fun getDirtyIds(): MutableSet<Int> {
        return prefs.getStringSet(KEY_DIRTY, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toMutableSet() ?: mutableSetOf()
    }

    private fun markDirty(id: Int) {
        val dirty = getDirtyIds()
        dirty.add(id)
        prefs.edit().putStringSet(KEY_DIRTY, dirty.map { it.toString() }.toSet()).apply()
    }

    private fun clearDirty(id: Int) {
        val dirty = getDirtyIds()
        dirty.remove(id)
        prefs.edit().putStringSet(KEY_DIRTY, dirty.map { it.toString() }.toSet()).apply()
    }

    // ------------------------------------------------------------------ //
    //  SYNCHRO
    // ------------------------------------------------------------------ //

    suspend fun syncFromServer(token: String): Result<List<Raid>> = withContext(Dispatchers.IO) {

        if (!isOnline()) {
            val local = loadLocally()
            return@withContext if (local.isNotEmpty())
                Result.success(local)
            else
                Result.failure(Exception("Hors ligne — aucune donnée"))
        }

        // 1. push local → serveur
        pushDirtyRaids(token)

        // 2. pull serveur → local
        return@withContext RaidApiService.getAllRaids(token).onSuccess { raids ->
            saveLocally(raids)
        }
    }

    private suspend fun pushDirtyRaids(token: String) {
        val dirtyIds = getDirtyIds()
        if (dirtyIds.isEmpty()) return

        val localRaids = loadLocally().toMutableList()

        dirtyIds.forEach { id ->
            val raid = localRaids.find { it.id == id } ?: return@forEach

            if (raid.id < 0) {
                // 🟢 CREATE (POST)
                RaidApiService.createRaid(raid.copy(id = 0), token)
                    .onSuccess { created ->
                        val index = localRaids.indexOfFirst { it.id == raid.id }
                        if (index != -1) {
                            localRaids[index] = created
                        }
                        clearDirty(raid.id)
                        saveLocally(localRaids)
                    }
                    .onFailure {
                        // garder dirty
                    }
            } else {
                // 🟢 UPDATE (PUT)
                RaidApiService.updateRaid(raid, token)
                    .onSuccess { updated ->
                        val index = localRaids.indexOfFirst { it.id == updated.id }
                        if (index != -1) {
                            localRaids[index] = updated
                        }
                        clearDirty(updated.id)
                        saveLocally(localRaids)
                    }
                    .onFailure {
                        // garder dirty
                    }
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  CREATE
    // ------------------------------------------------------------------ //

    suspend fun createRaid(raid: Raid, token: String): Result<Raid> =
        withContext(Dispatchers.IO) {

            val current = loadLocally().toMutableList()

            if (!isOnline()) {
                val tempId = -(System.currentTimeMillis() % 100000).toInt()
                val localRaid = raid.copy(id = tempId, isDirty = true)

                current.add(localRaid)
                saveLocally(current)
                markDirty(tempId)

                return@withContext Result.success(localRaid)
            }

            return@withContext RaidApiService.createRaid(raid, token)
                .onSuccess { created ->
                    current.add(created)
                    saveLocally(current)
                }
        }

    // ------------------------------------------------------------------ //
    //  UPDATE
    // ------------------------------------------------------------------ //

    suspend fun updateRaid(raid: Raid, token: String): Result<Raid> =
        withContext(Dispatchers.IO) {

            val current = loadLocally().toMutableList()
            val index = current.indexOfFirst { it.id == raid.id }

            if (!isOnline()) {
                val dirtyRaid = raid.copy(isDirty = true)
                if (index != -1) current[index] = dirtyRaid

                saveLocally(current)
                markDirty(raid.id)

                return@withContext Result.success(dirtyRaid)
            }

            return@withContext RaidApiService.updateRaid(raid, token)
                .onSuccess { updated ->
                    if (index != -1) current[index] = updated
                    saveLocally(current)
                    clearDirty(updated.id)
                }
        }

    // ------------------------------------------------------------------ //
    //  DELETE
    // ------------------------------------------------------------------ //

    suspend fun deleteRaid(id: Int, token: String): Result<Unit> =
        withContext(Dispatchers.IO) {

            val current = loadLocally().toMutableList()

            if (!isOnline()) {
                // suppression locale + sync plus tard (option bonus)
                current.removeIf { it.id == id }
                saveLocally(current)
                markDirty(id)
                return@withContext Result.success(Unit)
            }

            return@withContext RaidApiService.deleteRaid(id, token)
                .onSuccess {
                    current.removeIf { it.id == id }
                    saveLocally(current)
                    clearDirty(id)
                }
        }
}