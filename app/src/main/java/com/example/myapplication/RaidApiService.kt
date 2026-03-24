package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object RaidApiService {

    // ── GESTION DES SOURCES ────────────────────────────────────────────────
    enum class Source {
        RENDER, GITHUB
    }

    // Basculer sur GITHUB pour tester le mode lecture seule ou secours
    val SOURCE = Source.RENDER

    private val URL_RENDER = "https://androidstudio-1.onrender.com"
    private val URL_GITHUB = "https://raw.githubusercontent.com/ton-pseudo/ton-repo/main"

    private val BASE_URL = if (SOURCE == Source.RENDER) URL_RENDER else URL_GITHUB

    // ── PUBLIC API ─────────────────────────────────────────────────────────

    /**
     * Récupère tous les raids (GET)
     */
    suspend fun getAllRaids(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        runCatching {
            // Sur GitHub, le fichier s'appelle souvent raids.json
            val endpoint = if (SOURCE == Source.GITHUB) "/raids.json" else "/raids"

            val conn = openConnection(endpoint, "GET")
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code != 200) throw Exception("Erreur HTTP $code")

            val array = JSONArray(body)
            List(array.length()) { i -> Raid.fromJson(array.getJSONObject(i)) }
        }
    }

    /**
     * Crée un raid (POST) - Désactivé si source = GITHUB
     */
    suspend fun createRaid(raid: Raid): Result<Raid> = withContext(Dispatchers.IO) {
        if (SOURCE == Source.GITHUB) return@withContext Result.failure(Exception("Lecture seule sur GitHub"))

        runCatching {
            val conn = openConnection("/raids", "POST")
            writeBody(conn, raid.toJson().toString())

            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code !in 200..201) throw Exception("Échec création $code")
            Raid.fromJson(JSONObject(body))
        }
    }

    /**
     * Met à jour un raid (PUT)
     */
    suspend fun updateRaid(raid: Raid): Result<Raid> = withContext(Dispatchers.IO) {
        if (SOURCE == Source.GITHUB) return@withContext Result.failure(Exception("Lecture seule sur GitHub"))

        runCatching {
            val conn = openConnection("/raids/${raid.id}", "PUT")
            writeBody(conn, raid.toJson().toString())

            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code != 200) throw Exception("Échec mise à jour $code")
            Raid.fromJson(JSONObject(body))
        }
    }

    /**
     * Supprime un raid (DELETE)
     */
    suspend fun deleteRaid(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (SOURCE == Source.GITHUB) return@withContext Result.failure(Exception("Lecture seule sur GitHub"))

        runCatching {
            val conn = openConnection("/raids/$id", "DELETE")
            val code = conn.responseCode
            conn.disconnect()

            if (code !in 200..204) throw Exception("Échec suppression $code")
            Unit
        }
    }

    // ── UTILITAIRES (HTTP STANDARD) ────────────────────────────────────────

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val conn = URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        // Indispensable pour POST et PUT
        if (method == "POST" || method == "PUT") {
            conn.doOutput = true
        }

        return conn
    }

    private fun readBody(conn: HttpURLConnection): String {
        return try {
            val stream = if (conn.responseCode < 400) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun writeBody(conn: HttpURLConnection, body: String) {
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
    }
}