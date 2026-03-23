package com.example.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class RaidRepository(private val context: Context) {

    private val prefs   = context.getSharedPreferences("raids_prefs", Context.MODE_PRIVATE)
    private val KEY_RAIDS = "raids_json"
    private val KEY_DIRTY = "dirty_ids"

    fun isOnline(): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ── Persistance locale ─────────────────────────────────────────────────
    private fun saveLocally(raids: List<Raid>) {
        val array = JSONArray().also { arr -> raids.forEach { arr.put(it.toJson()) } }
        prefs.edit().putString(KEY_RAIDS, array.toString()).apply()
    }

    fun loadLocally(): List<Raid> {
        val str = prefs.getString(KEY_RAIDS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(str)
            List(arr.length()) { Raid.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    // ── Dirty IDs ──────────────────────────────────────────────────────────
    private fun getDirtyIds(): MutableSet<Int> =
        prefs.getStringSet(KEY_DIRTY, emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()

    private fun markDirty(id: Int) {
        val s = getDirtyIds().also { it.add(id) }
        prefs.edit().putStringSet(KEY_DIRTY, s.map { it.toString() }.toSet()).apply()
    }

    private fun clearDirty(id: Int) {
        val s = getDirtyIds().also { it.remove(id) }
        prefs.edit().putStringSet(KEY_DIRTY, s.map { it.toString() }.toSet()).apply()
    }

    // ── Synchro ────────────────────────────────────────────────────────────
    suspend fun syncFromServer(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            val local = loadLocally()
            return@withContext if (local.isNotEmpty()) Result.success(local)
            else Result.failure(Exception("Hors ligne — aucune donnée locale"))
        }
        pushDirtyRaids()
        RaidApiService.getAllRaids().onSuccess { saveLocally(it) }
    }

    private suspend fun pushDirtyRaids() {
        val dirtyIds   = getDirtyIds()
        if (dirtyIds.isEmpty()) return
        val localRaids = loadLocally().toMutableList()
        var changed    = false

        dirtyIds.forEach { id ->
            val raid = localRaids.find { it.id == id } ?: return@forEach
            if (raid.id < 0) {
                RaidApiService.createRaid(raid.copy(id = 0)).onSuccess { created ->
                    val idx = localRaids.indexOfFirst { it.id == raid.id }
                    if (idx != -1) localRaids[idx] = created
                    clearDirty(id)
                    changed = true
                }
            } else {
                RaidApiService.updateRaid(raid).onSuccess { updated ->
                    val idx = localRaids.indexOfFirst { it.id == updated.id }
                    if (idx != -1) localRaids[idx] = updated
                    clearDirty(id)
                    changed = true
                }
            }
        }
        if (changed) saveLocally(localRaids)
    }

    // ── CRUD ───────────────────────────────────────────────────────────────
    suspend fun createRaid(raid: Raid): Result<Raid> = withContext(Dispatchers.IO) {
        val current = loadLocally().toMutableList()
        if (!isOnline()) {
            val tempId    = -(System.currentTimeMillis() % 100_000).toInt()
            val localRaid = raid.copy(id = tempId, isDirty = true)
            current.add(localRaid)
            saveLocally(current)
            markDirty(tempId)
            return@withContext Result.success(localRaid)
        }
        RaidApiService.createRaid(raid).onSuccess { current.add(it); saveLocally(current) }
    }

    suspend fun updateRaid(raid: Raid): Result<Raid> = withContext(Dispatchers.IO) {
        val current = loadLocally().toMutableList()
        val index   = current.indexOfFirst { it.id == raid.id }
        if (!isOnline()) {
            val dirty = raid.copy(isDirty = true)
            if (index != -1) current[index] = dirty
            saveLocally(current)
            markDirty(raid.id)
            return@withContext Result.success(dirty)
        }
        RaidApiService.updateRaid(raid).onSuccess { updated ->
            if (index != -1) current[index] = updated
            saveLocally(current)
            clearDirty(updated.id)
        }
    }

    suspend fun deleteRaid(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadLocally().toMutableList()
        if (!isOnline()) {
            current.removeIf { it.id == id }
            saveLocally(current)
            return@withContext Result.success(Unit)
        }
        RaidApiService.deleteRaid(id).onSuccess {
            current.removeIf { it.id == id }
            saveLocally(current)
            clearDirty(id)
        }
    }
}