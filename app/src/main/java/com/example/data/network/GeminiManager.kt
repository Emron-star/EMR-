package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiSignal(
    val action: String, // "RISE", "FALL", "HOLD"
    val confidence: Double, // 0.0 to 1.0
    val explanation: String
)

class GeminiManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getAiSignal(ticks: List<Double>, symbolLabel: String): AiSignal = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            Log.d("GeminiManager", "Gemini API key is unconfigured. Falling back to local quantitative algorithm.")
            return@withContext getLocalQuantitativeSignal(ticks, symbolLabel)
        }

        val prompt = """
            You are a high-frequency algorithmic quantitative trading model integrated into the Deriv broker platform.
            Analyze the following historical tick prices for $symbolLabel in chronological order:
            ${ticks.joinToString(", ")}

            You MUST make a trading decision for the next 10-second contract.
            Your output MUST be a single, strict, valid JSON object with EXACTLY these fields:
            - "action": either "RISE" (if price is highly likely to go up), "FALL" (if price is highly likely to go down), or "HOLD" (if neutral/unclear)
            - "confidence": decimal between 0.0 and 1.0 representing your probability score
            - "explanation": a single concise technical sentence explaining the indicator behavior (e.g. SMA cross, RSI divergence)

            Do NOT wrap the JSON in Markdown block markings like ```json. Do NOT include any text outside of the raw JSON.
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            // Add generation config to enforce JSON format
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d("GeminiManager", "Response: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val text = parts.getJSONObject(0).getString("text").trim()

                    val signalJson = JSONObject(text)
                    val action = signalJson.optString("action", "HOLD").uppercase()
                    val confidence = signalJson.optDouble("confidence", 0.5)
                    val explanation = signalJson.optString("explanation", "Trend analysis completed.")

                    return@withContext AiSignal(
                        action = if (action in listOf("RISE", "FALL", "HOLD")) action else "HOLD",
                        confidence = confidence,
                        explanation = explanation
                    )
                } else {
                    Log.e("GeminiManager", "Error calling Gemini API: ${response.code} ${response.message}")
                    return@withContext getLocalQuantitativeSignal(ticks, symbolLabel)
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiManager", "Exception in Gemini: ${e.message}")
            return@withContext getLocalQuantitativeSignal(ticks, symbolLabel)
        }
    }

    suspend fun analyzeMarket(ticks: List<Double>, symbolLabel: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            return@withContext getLocalMarketAnalysis(ticks, symbolLabel)
        }

        val prompt = """
            You are DTNexus AI, a premium automated trading platform assistant.
            The user is observing the $symbolLabel symbol, which is currently at a price of ${ticks.lastOrNull() ?: "unknown"}.
            Here is the chronological array of the last 15 tick prices: ${ticks.joinToString(", ")}.
            Write an elegant, 2-3 sentence market commentary detailing the short-term trend, resistance/support indications, and momentum behavior (e.g. RSI level, SMA alignment) to help the user configure their automated bots. Keep it highly technical, confident, and professional. Avoid warnings or disclaimers.
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val text = parts.getJSONObject(0).getString("text").trim()
                    return@withContext text
                } else {
                    return@withContext getLocalMarketAnalysis(ticks, symbolLabel)
                }
            }
        } catch (e: Exception) {
            return@withContext getLocalMarketAnalysis(ticks, symbolLabel)
        }
    }

    // High fidelity offline fallback quantitative signal generator
    private fun getLocalQuantitativeSignal(ticks: List<Double>, symbolLabel: String): AiSignal {
        if (ticks.size < 5) {
            return AiSignal("HOLD", 0.50, "Awaiting sufficient tick data for quantitative calculation.")
        }
        val lastPrice = ticks.last()
        val smaShort = ticks.takeLast(3).average()
        val smaLong = ticks.takeLast(5).average()

        return if (smaShort > smaLong) {
            AiSignal("RISE", 0.76, "SMA(3) crossed above SMA(5) on $symbolLabel, signaling strong upward momentum.")
        } else if (smaShort < smaLong) {
            AiSignal("FALL", 0.72, "SMA(3) crossed below SMA(5) on $symbolLabel, confirming downward trend structure.")
        } else {
            AiSignal("HOLD", 0.55, "Market is currently moving sideways; indicators are neutral.")
        }
    }

    private fun getLocalMarketAnalysis(ticks: List<Double>, symbolLabel: String): String {
        if (ticks.size < 5) return "Initializing market quantitative engine. Analyzing $symbolLabel real-time tick flow..."
        val last = ticks.last()
        val sma5 = ticks.takeLast(5).average()
        val trend = if (last > sma5) "Bullish Ascending" else "Bearish Descending"
        val changePercent = if (ticks.first() != 0.0) ((last - ticks.first()) / ticks.first()) * 100 else 0.0
        val changeLabel = String.format("%.4f%%", changePercent)

        return "$symbolLabel is exhibiting a strong $trend structure, currently trading at $last. Moving average (SMA-5) is aligned at ${String.format("%.4f", sma5)}, confirming an active trend breakout of $changeLabel over the sliding window. Traders are advised to monitor momentum barriers before execution."
    }
}
