package com.chujai888.pdftoconverter888

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val url: String, val notes: String)

/** ตรวจสอบเวอร์ชันใหม่จาก GitHub Releases อัตโนมัติ (ไม่ส่งข้อมูลผู้ใช้ใดๆ) */
object Updater {
    const val REPO = "lovenongb888-max/PDF-to-Converter888"

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (conn.responseCode != 200) return@withContext null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val latest = json.optString("tag_name").removePrefix("v").removePrefix("V")
                if (latest.isEmpty() || !isNewer(latest, currentVersion)) return@withContext null

                var url = json.optString("html_url")
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                            url = a.optString("browser_download_url", url)
                            break
                        }
                    }
                }
                UpdateInfo(latest, url, json.optString("body"))
            } finally {
                conn.disconnect()
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val a = latest.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
