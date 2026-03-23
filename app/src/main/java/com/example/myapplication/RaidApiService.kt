package com.example.myapplication

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object RaidApiService {

    private const val TAG = "RaidApiService"
    private const val BASE_URL = "https://g6-devc3.unicaen.fr/api"

    // ------------------------------------------------------------------ //
    //  1. AUTHENTIFICATION (POST)
    // ------------------------------------------------------------------ //
    fun login(email: String, password: String): Result<String> {
        return runCatching {
            val conn = openConnection("/login", "POST")
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            writeBody(conn, json.toString())
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code != 200) error("Login failed ($code)")
            JSONObject(body).getString("token")
        }
    }

    // ------------------------------------------------------------------ //
    //  2. LECTURE (GET) - Verbe 1
    // ------------------------------------------------------------------ //
    fun getAllRaids(token: String): Result<List<Raid>> {
        return runCatching {
            val conn = openConnection("/raids", "GET", token)
            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code != 200) error("HTTP $code")
            val array = JSONObject(body).getJSONArray("raids")
            List(array.length()) { i -> Raid.fromJson(array.getJSONObject(i)) }
        }
    }

    // ------------------------------------------------------------------ //
    //  3. CRÉATION (POST) - Verbe 2 (Option A)
    // ------------------------------------------------------------------ //
    fun createRaid(raid: Raid, token: String): Result<Raid> {
        return runCatching {
            val conn = openConnection("/raids", "POST", token)
            writeBody(conn, raid.toJson().toString())

            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code !in 200..201) error("Create failed $code: $body")
            Raid.fromJson(JSONObject(body))
        }
    }

    // ------------------------------------------------------------------ //
    //  4. MODIFICATION (PUT) - Verbe 2 (Option B)
    // ------------------------------------------------------------------ //
    fun updateRaid(raid: Raid, token: String): Result<Raid> {
        return runCatching {
            val conn = openConnection("/raids/${raid.id}", "PUT", token)
            writeBody(conn, raid.toJson().toString())

            val code = conn.responseCode
            val body = readBody(conn)
            conn.disconnect()

            if (code != 200) error("Update failed $code: $body")
            Raid.fromJson(JSONObject(body))
        }
    }

    // ------------------------------------------------------------------ //
    //  5. SUPPRESSION (DELETE)
    // ------------------------------------------------------------------ //
    fun deleteRaid(id: Int, token: String): Result<Unit> {
        return runCatching {
            val conn = openConnection("/raids/$id", "DELETE", token)
            val code = conn.responseCode
            conn.disconnect()
            if (code !in 200..204) error("Delete failed $code")
        }
    }

    // ------------------------------------------------------------------ //
    //  UTILITAIRES RÉSEAU
    // ------------------------------------------------------------------ //

    private fun openConnection(endpoint: String, method: String, token: String? = null): HttpURLConnection {
        val conn = URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        conn.connectTimeout = 10000
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