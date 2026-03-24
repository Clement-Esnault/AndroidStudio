package com.example.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RaidRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("raids_prefs", Context.MODE_PRIVATE)
    private val KEY_RAIDS = "raids_json"
    private val KEY_DIRTY = "dirty_ids"
    private val KEY_DELETED = "deleted_ids"

    // ── Vérification Réseau ────────────────────────────────────────────────
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ── Persistance locale ─────────────────────────────────────────────────
    private fun saveLocally(raids: List<Raid>) {
        val array = JSONArray()
        raids.forEach { raid ->
            // On convertit l'objet complet en JSON pour le stockage local
            val obj = raid.toJson().apply {
                put("RAI_ID", raid.id) // On force l'ID car toJson() de base ne le met pas (pour l'API)
                put("local_is_dirty", raid.isDirty)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RAIDS, array.toString()).apply()
    }

    fun loadLocally(): List<Raid> {
        val str = prefs.getString(KEY_RAIDS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                Raid.fromJson(obj).copy(
                    id = obj.optInt("RAI_ID"),
                    isDirty = obj.optBoolean("local_is_dirty", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Gestion des listes de synchronisation (Dirty & Deleted) ────────────
    private fun getIds(key: String): MutableSet<String> =
        prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun markDirty(id: Int) = prefs.edit().putStringSet(KEY_DIRTY, getIds(KEY_DIRTY).apply { add(id.toString()) }).apply()
    private fun clearDirty(id: Int) = prefs.edit().putStringSet(KEY_DIRTY, getIds(KEY_DIRTY).apply { remove(id.toString()) }).apply()

    private fun markDeleted(id: Int) = prefs.edit().putStringSet(KEY_DELETED, getIds(KEY_DELETED).apply { add(id.toString()) }).apply()
    private fun clearDeleted(id: Int) = prefs.edit().putStringSet(KEY_DELETED, getIds(KEY_DELETED).apply { remove(id.toString()) }).apply()

    // ── Synchronisation ────────────────────────────────────────────────────
    suspend fun sync(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            val local = loadLocally()
            return@withContext if (local.isNotEmpty()) Result.success(local)
            else Result.failure(Exception("Hors ligne et aucun cache"))
        }

        // 1. Envoyer les suppressions en attente
        getIds(KEY_DELETED).forEach { idStr ->
            RaidApiService.deleteRaid(idStr.toInt()).onSuccess { clearDeleted(idStr.toInt()) }
        }

        // 2. Envoyer les créations/modifications (Dirty)
        pushDirtyRaids()

        // 3. Récupérer les données fraîches du serveur
        RaidApiService.getAllRaids().onSuccess { serverRaids ->
            val localRaids = loadLocally()

            // Logique de MERGE : On garde les versions locales si elles sont "DIRTY"
            val mergedList = serverRaids.map { sRaid ->
                localRaids.find { it.id == sRaid.id && it.isDirty } ?: sRaid
            }.toMutableList()

            // On ajoute aussi les nouveaux raids créés en offline (ID < 0) qui n'auraient pas encore sync
            mergedList.addAll(localRaids.filter { it.id < 0 })

            saveLocally(mergedList)
            return@withContext Result.success(mergedList)
        }
    }

    private suspend fun pushDirtyRaids() {
        val dirtyIds = getIds(KEY_DIRTY)
        val localRaids = loadLocally().toMutableList()

        dirtyIds.forEach { idStr ->
            val id = idStr.toInt()
            val raid = localRaids.find { it.id == id } ?: return@forEach

            if (id < 0) { // Création
                RaidApiService.createRaid(raid).onSuccess { created ->
                    localRaids.removeIf { it.id == id }
                    localRaids.add(created)
                    clearDirty(id)
                }
            } else { // Mise à jour
                RaidApiService.updateRaid(raid).onSuccess { updated ->
                    val idx = localRaids.indexOfFirst { it.id == updated.id }
                    if (idx != -1) localRaids[idx] = updated
                    clearDirty(id)
                }
            }
        }
        saveLocally(localRaids)
    }

    // ── CRUD ───────────────────────────────────────────────────────────────
    suspend fun create(raid: Raid) = withContext(Dispatchers.IO) {
        // On génère un ID temporaire négatif
        val tempId = -(System.currentTimeMillis() % 100000).toInt()
        val localRaid = raid.copy(id = tempId, isDirty = true)

        val current = loadLocally().toMutableList()
        current.add(localRaid)
        saveLocally(current)
        markDirty(tempId) // On le marque pour le bouton "Sync" plus tard
    }

    suspend fun update(raid: Raid) = withContext(Dispatchers.IO) {
        val current = loadLocally().toMutableList()
        val index = current.indexOfFirst { it.id == raid.id }

        if (index != -1) {
            // On marque l'objet comme sale (Dirty)
            current[index] = raid.copy(isDirty = true)
            saveLocally(current)
            markDirty(raid.id) // On le marque pour le bouton "Sync"
        }
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val current = loadLocally().filter { it.id != id }
        saveLocally(current)

        // Si l'ID est positif (existe sur serveur), on le note pour suppression future
        if (id > 0) {
            markDeleted(id)
        }
        clearDirty(id) // Plus besoin de l'updater s'il est supprimé
    }
    suspend fun getLocalRaids(): List<Raid> = withContext(Dispatchers.IO) {
        loadLocally()
    }

    // Cette fonction est celle appelée par ton bouton "Sync"
    suspend fun pushAndFetch(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext Result.failure(Exception("Pas de réseau"))

        // 1. On pousse les suppressions
        getIds(KEY_DELETED).forEach { idStr ->
            RaidApiService.deleteRaid(idStr.toInt()).onSuccess { clearDeleted(idStr.toInt()) }
        }

        // 2. On pousse les modifs/créations
        pushDirtyRaids()

        // 3. On récupère les dernières données du serveur (le Fetch)
        RaidApiService.getAllRaids().onSuccess { serverRaids ->
            val localRaids = loadLocally()

            // Merge : On garde le local si c'est encore "Dirty" (échec sync partiel)
            val mergedList = serverRaids.map { sRaid ->
                localRaids.find { it.id == sRaid.id && it.isDirty } ?: sRaid
            }.toMutableList()

            mergedList.addAll(localRaids.filter { it.id < 0 })
            saveLocally(mergedList)
            return@withContext Result.success(mergedList)
        }
    }
}