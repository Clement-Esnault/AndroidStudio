package com.example.myapplication

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object RaidApiService {

    private const val TAG      = "RaidApiService"
    private const val BASE_URL = "https://g6-devc3.unicaen.fr/api"

    fun getAllRaids(): Result<List<Raid>> = runCatching {
        val conn = openConnection("/raids/manage", "GET")
        val code = conn.responseCode
        val body = readBody(conn)
        conn.disconnect()
        if (code != 200) error("HTTP $code : $body")
        val array = org.json.JSONArray(body)
        List(array.length()) { i -> Raid.fromJson(array.getJSONObject(i)) }
    }

    fun createRaid(raid: Raid): Result<Raid> = runCatching {
        val conn = openConnection("/raids", "POST")
        writeBody(conn, raid.toJson().toString())
        val code = conn.responseCode
        val body = readBody(conn)
        conn.disconnect()
        if (code !in 200..201) error("Create failed $code: $body")
        Raid.fromJson(JSONObject(body))
    }

    fun updateRaid(raid: Raid): Result<Raid> = runCatching {
        val conn = openConnection("/raids/${raid.id}", "PUT")
        writeBody(conn, raid.toJson().toString())
        val code = conn.responseCode
        val body = readBody(conn)
        conn.disconnect()
        if (code != 200) error("Update failed $code: $body")
        Raid.fromJson(JSONObject(body))
    }

    fun deleteRaid(id: Int): Result<Unit> = runCatching {
        val conn = openConnection("/raids/$id", "DELETE")
        val code = conn.responseCode
        conn.disconnect()
        if (code !in 200..204) error("Delete failed $code")
    }

    // ── Utilitaires ────────────────────────────────────────────────────────
    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val conn = URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        // Basic Auth — encode "admin:admin" en base64
        val credentials = android.util.Base64.encodeToString(
            "admin:admin".toByteArray(),
            android.util.Base64.NO_WRAP
        )
        conn.setRequestProperty("Authorization", "Basic $credentials")

        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
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