package com.example.myapplication

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object RaidApiService {

    private const val BASE_URL = "https://g6-devc3.unicaen.fr/api"
    private const val FAUX_API = true

    // ------------------------------------------------------------------ //
    //  Faux API, je peux pas m'y co
    // ------------------------------------------------------------------ //

    private val mockRaids = mutableListOf(
        Raid(id = 1, clubId = 1, nom = "Raid O'Bivwak",
            lieu = "Parc des Noues", dateDebut = "2026-05-23",
            dateFin = "2026-05-24", status = "VALIDE"),
        Raid(id = 2, clubId = 1, nom = "RAID CHAMPETRE",
            lieu = "Parc Intercommunal Debreuil", dateDebut = "2025-11-01",
            dateFin = "2025-11-02", status = "VALIDE")
    )
    private var nextMockId = 3

    // ------------------------------------------------------------------ //
    //  Utilitaires réseau
    // ------------------------------------------------------------------ //

    private fun openConnection(
        endpoint: String,
        method: String,
        token: String? = null
    ): HttpURLConnection {
        val conn = URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        return conn
    }

    private fun readBody(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode < 400) conn.inputStream else conn.errorStream
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun writeBody(conn: HttpURLConnection, body: String) {
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
    }

    // ------------------------------------------------------------------ //
    //  GET /raids
    // ------------------------------------------------------------------ //

    fun getAllRaids(): Result<List<Raid>> {
        if (FAUX_API) return Result.success(mockRaids.toList())

        return runCatching {
            val conn = openConnection("/raids", "GET")
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code != 200) error("Erreur HTTP $code : $body")

            val array: JSONArray = JSONObject(body).getJSONArray("raids")
            List(array.length()) { i -> Raid.fromJson(array.getJSONObject(i)) }
        }
    }

    // ------------------------------------------------------------------ //
    //  POST /raids  →  créer
    // ------------------------------------------------------------------ //

    fun createRaid(raid: Raid, token: String): Result<Raid> {
        if (FAUX_API) {
            val newRaid = raid.copy(id = nextMockId++)
            mockRaids.add(newRaid)
            return Result.success(newRaid)
        }

        return runCatching {
            val conn = openConnection("/raids", "POST", token)
            writeBody(conn, raid.toJson().toString())
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()
            if (code !in 200..201) error("Erreur HTTP $code : $body")
            Raid.fromJson(JSONObject(body))
        }
    }

    // ------------------------------------------------------------------ //
    //  PUT /raids/{id}  →  modifier
    // ------------------------------------------------------------------ //

    fun updateRaid(raid: Raid, token: String): Result<Raid> {
        if (FAUX_API) {
            val index = mockRaids.indexOfFirst { it.id == raid.id }
            if (index == -1) return Result.failure(Exception("Raid introuvable"))
            mockRaids[index] = raid
            return Result.success(raid)
        }

        return runCatching {
            val conn = openConnection("/raids/${raid.id}", "PUT", token)
            writeBody(conn, raid.toJson().toString())
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()
            if (code != 200) error("Erreur HTTP $code : $body")
            Raid.fromJson(JSONObject(body))
        }
    }

    // ------------------------------------------------------------------ //
    //  DELETE /raids/{id}  →  supprimer
    // ------------------------------------------------------------------ //

    fun deleteRaid(id: Int, token: String): Result<Unit> {
        if (FAUX_API) {
            val removed = mockRaids.removeIf { it.id == id }
            return if (removed) Result.success(Unit)
            else Result.failure(Exception("Raid introuvable"))
        }

        return runCatching {
            val conn = openConnection("/raids/$id", "DELETE", token)
            val code = conn.responseCode
            readBody(conn)
            conn.disconnect()
            if (code !in 200..204) error("Erreur HTTP $code")
        }
    }
}