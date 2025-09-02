package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PhishTankChecker {

    private const val API_URL = "https://checkurl.phishtank.com/checkurl/"
    private const val TAG = "PhishTankChecker"

    /**
     * Check a URL against PhishTank.
     *
     * @param url      The URL you want to verify. It will be URL‑encoded automatically.
     * @param appKey   Your application key (optional). Without it you’ll hit the rate limit faster.
     *
     * @return Either a [PhishResult.Success] containing the parsed data or a [PhishResult.Error]
     *         explaining why the request failed.
     */
    @JvmStatic
    fun checkUrl(
        url: String,
        appKey: String? = null
    ): PhishResult {
        try {
            // Build the POST body
            val form = StringBuilder()
            form.append("url=").append(encode(url))
            form.append("&format=").append(encode("json"))
            if (!appKey.isNullOrBlank()) {
                form.append("&app_key=").append(encode(appKey))
            }

            val body = form.toString()

            Log.d(TAG, "Checking PhishTank for $url: Body: $body")

            // Open connection
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", "phishtank/SEQR/1.0")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept",  "application/json")

            // Write request body
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            // Check HTTP status
            val responseCode = conn.responseCode
            return if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse body
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val data =  parseJson(response)
                Log.d(TAG, "PhishTank result for $url: $data")
                PhishResult.Success(data)
            } else {
                // Other HTTP errors
                val errorMsg = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: "Unexpected HTTP $responseCode"
                PhishResult.Error(message = errorMsg)
            }
        } catch (e: Exception) {
            return PhishResult.Error(message = e.message ?: "Unknown error")
        }
    }

    @JvmStatic
    fun isPhish(result: PhishResult): Boolean {
        return result is PhishResult.Success && isPhish(result.data)
    }

    @JvmStatic
    fun isPhish(data: PhishData): Boolean {
        return data.inDatabase && data.valid != null && data.valid && data.verified != null && data.verified
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    data class PhishData(
        val url: String,
        val inDatabase: Boolean,
        val phishId: Int?,
        val phishDetailPage: String?,
        val verified: Boolean?,
        val verifiedAt: String?,
        val valid: Boolean?,
        val submittedAt: String?
    )

    /** Result type – success or error */
    sealed class PhishResult {
        data class Success(
            val data: PhishData
        ) : PhishResult()

        data class Error(
            val message: String
        ) : PhishResult()
    }

    private fun parseJson(json: String): PhishData {
        // Using Android's built‑in org.json
        val jo = JSONObject(json)
        val results = jo.optJSONObject("results") ?: JSONObject()
        return PhishData(
            url = results.optString("url", ""),
            inDatabase = results.optBoolean("in_database", false),
            phishId = results.optInt("phish_id", -1).takeIf { it != -1 },
            phishDetailPage = results.optString("phish_detail_page", "").takeIf { it.isNotEmpty() },
            verified = parseBooleanValue(results.optString("verified", "")),
            verifiedAt = results.optString("verified_at", ""),
            valid = parseBooleanValue(results.optString("valid", "")),
            submittedAt = results.optString("submitted_at", "")
        )
    }

    /** Helper – converts “y”, “yes”, “true”, “1” → true, everything else → false */
    private fun parseBooleanValue(value: String): Boolean? {
        return when (value.lowercase()) {
            "y", "yes", "true", "1" -> true
            "n", "no", "false", "0" -> false
            else -> null
        }
    }
}