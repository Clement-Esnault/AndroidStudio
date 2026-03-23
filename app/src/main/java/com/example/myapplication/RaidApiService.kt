package com.example.myapplication

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object RaidApiService {

    private var authToken: String? = null
    enum class Source { G6, GITHUB }

    // 🔹 Changez ici pour switcher entre GitHub et G6
    val SOURCE = Source.G6

    private val BASE_URL: String
        get() = when (SOURCE) {
            Source.G6 -> "https://g6-devc3.unicaen.fr/api"
            Source.GITHUB -> "https://my-json-server.typicode.com/Clement-Esnault/AndroidStudio/db"
        }

    // ── PUBLIC API ─────────────────────────────────────────────────────────
    suspend fun getAllRaids(): Result<List<Raid>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = when (SOURCE) {
                Source.G6 -> {
                    val conn = openConnection("/raids/manage", "GET")
                    val code = conn.responseCode
                    val resp = readBody(conn)
                    conn.disconnect()
                    if (code != 200) error("HTTP $code : $resp")
                    resp
                }
                Source.GITHUB -> {
                    // GitHub RAW: lecture seule du JSON
                    URL(BASE_URL).openStream().bufferedReader().use { it.readText() }
                }
            }

            val raidsJsonArray = try {
                when (SOURCE) {
                    Source.G6 -> JSONArray(body)
                    Source.GITHUB -> JSONObject(body).getJSONArray("raids")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                JSONArray() // retourne vide si erreur
            }

            List(raidsJsonArray.length()) { i ->
                Raid.fromJson(raidsJsonArray.getJSONObject(i))
            }
        }
    }

    suspend fun createRaid(raid: Raid): Result<Raid> = withContext(Dispatchers.IO) {
        runCatching {
            if (SOURCE == Source.GITHUB) error("Create not supported on GitHub JSON")

            val conn = openConnection("/raids", "POST")
            writeBody(conn, raid.toJson().toString())
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()
            if (code !in 200..201) error("Create failed $code: $body")
            Raid.fromJson(JSONObject(body))
        }
    }

    suspend fun updateRaid(raid: Raid): Result<Raid> = withContext(Dispatchers.IO) {
        runCatching {
            if (SOURCE == Source.GITHUB) error("Update not supported on GitHub JSON")

            val conn = openConnection("/raids/${raid.id}", "PUT")
            writeBody(conn, raid.toJson().toString())
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()
            if (code != 200) error("Update failed $code: $body")
            Raid.fromJson(JSONObject(body))
        }
    }

    suspend fun deleteRaid(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (SOURCE == Source.GITHUB) error("Delete not supported on GitHub JSON")

            val conn = openConnection("/raids/$id", "DELETE")
            val code = conn.responseCode
            conn.disconnect()
            if (code !in 200..204) error("Delete failed $code")
        }
    }

    // ── UTILITAIRES ────────────────────────────────────────────────────────

    suspend fun login(email: String = "admin", mdp: String = "admin"): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Construction de l'URL (vérifiez que BASE_URL ne finit pas par un slash)
            val url = URL("${BASE_URL}/login")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST" // Impératif : la route /login n'accepte que le POST
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            // Créer le JSON d'authentification tel qu'attendu par votre contrôleur Laravel
            val credentials = JSONObject().apply {
                put("email", email)      // Laravel attend souvent 'email' ou 'login'
                put("password", mdp)
            }

            writeBody(conn, credentials.toString())

            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code == 200) {
                val json = JSONObject(body)
                // Dans Laravel Sanctum, la clé est souvent 'token' ou 'access_token'
                val token = json.getString("token")
                authToken = token // Stockage pour openConnection()
                token
            } else {
                error("Échec Login $code: $body")
            }
        }
    }

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val conn = URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection
        conn.requestMethod = method

        // Headers standards [cite: 19]
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        // Si on a un jeton, on l'ajoute à CHAQUE requête suivante
        authToken?.let {
            conn.setRequestProperty("Authorization", "Bearer $it")
        }

        return conn
    }

    private fun readBody(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode < 400) conn.inputStream else conn.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private fun writeBody(conn: HttpURLConnection, body: String) {
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
    }
}