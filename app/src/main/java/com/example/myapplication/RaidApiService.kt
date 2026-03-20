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

    fun getAllRaids(): Result<List<Raid>> = runCatching {
        val conn = openConnection("/raids", "GET")
        val code = conn.responseCode
        val body = readBody(conn)
        conn.disconnect()

        if (code != 200) error("Erreur HTTP $code : $body")

        // L'API retourne { "raids": [...], "ageCategories": [...], ... }
        // On extrait uniquement le tableau "raids"
        val array: JSONArray = JSONObject(body).getJSONArray("raids")
        List(array.length()) { i -> Raid.fromJson(array.getJSONObject(i)) }
    }


    fun updateRaid(raid: Raid, token: String): Result<Raid> = runCatching {
        val conn = openConnection("/raids/${raid.id}", "PUT", token)
        writeBody(conn, raid.toJson().toString())

        val code = conn.responseCode
        val body = readBody(conn)
        conn.disconnect()

        if (code != 200) error("Erreur HTTP $code : $body")

        Raid.fromJson(JSONObject(body))
    }
}