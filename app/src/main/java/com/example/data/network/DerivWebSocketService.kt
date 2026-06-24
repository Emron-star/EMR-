package com.example.data.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class TickData(
    val symbol: String,
    val quote: Double,
    val bid: Double,
    val ask: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AuthState {
    IDLE,
    AUTHENTICATING,
    AUTHENTICATED,
    FAILED
}

class DerivWebSocketService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var activeSymbol: String = "R_10"
    private var activeAppId: String = "1089"
    private var activeToken: String = ""
    private var isConnecting = false
    private var isConnected = false

    private val _tickFlow = MutableSharedFlow<TickData>(replay = 1)
    val tickFlow: SharedFlow<TickData> = _tickFlow.asSharedFlow()

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Base prices for symbols to make the simulation highly realistic
    private val basePrices = mapOf(
        "R_10" to 7500.0,
        "R_100" to 12500.0,
        "frxEURUSD" to 1.0850,
        "cryBTCUSD" to 68450.0,
        "frxXAUUSD" to 2350.0
    )

    private val pipSizes = mapOf(
        "R_10" to 2,
        "R_100" to 2,
        "frxEURUSD" to 5,
        "cryBTCUSD" to 2,
        "frxXAUUSD" to 2
    )

    private var currentPrice = 7500.0

    @Synchronized
    fun connect(appId: String, apiToken: String, symbol: String) {
        if (activeSymbol != symbol || activeAppId != appId || activeToken != apiToken) {
            webSocket?.close(1000, "Configuration changed")
            isConnected = false
            _authState.value = AuthState.IDLE
        }

        activeSymbol = symbol
        activeAppId = appId.ifEmpty { "1089" }
        activeToken = apiToken
        currentPrice = basePrices[symbol] ?: 100.0

        // Stop any running simulation
        stopSimulation()

        val url = "wss://ws.binaryws.com/websockets/v3?app_id=$activeAppId"
        Log.d("DerivWebSocketService", "Connecting to: $url with token length: ${apiToken.length}")
        val request = Request.Builder().url(url).build()

        isConnecting = true
        if (apiToken.isNotEmpty()) {
            _authState.value = AuthState.AUTHENTICATING
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting = false
                isConnected = true
                Log.d("DerivWebSocketService", "WebSocket Opened!")

                if (activeToken.isNotEmpty()) {
                    // Send authorize request first
                    val authPayload = JSONObject().apply {
                        put("authorize", activeToken)
                    }
                    Log.d("DerivWebSocketService", "Sending authorization request...")
                    webSocket.send(authPayload.toString())
                } else {
                    // Subscribe directly
                    subscribeToTicks(webSocket, activeSymbol)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val msgType = json.optString("msg_type")

                    when (msgType) {
                        "authorize" -> {
                            if (json.has("error")) {
                                val errorObj = json.getJSONObject("error")
                                val errorMsg = errorObj.optString("message", "Unknown authorization error")
                                Log.e("DerivWebSocketService", "Authorization failed: $errorMsg")
                                _authState.value = AuthState.FAILED
                                // Start simulated ticks on authorization failure
                                startSimulation(activeSymbol)
                            } else {
                                Log.d("DerivWebSocketService", "Authorized successfully!")
                                _authState.value = AuthState.AUTHENTICATED
                                // Now subscribe to ticks after successful auth
                                subscribeToTicks(webSocket, activeSymbol)
                            }
                        }
                        "tick" -> {
                            val tickObj = json.getJSONObject("tick")
                            val quote = tickObj.getDouble("quote")
                            val bid = tickObj.optDouble("bid", quote - 0.01)
                            val ask = tickObj.optDouble("ask", quote + 0.01)
                            val rSymbol = tickObj.getString("symbol")

                            currentPrice = quote

                            scope.launch {
                                _tickFlow.emit(TickData(rSymbol, quote, bid, ask))
                            }
                        }
                        else -> {
                            if (json.has("error")) {
                                val errorObj = json.getJSONObject("error")
                                val errorMsg = errorObj.optString("message")
                                Log.e("DerivWebSocketService", "Error response of type $msgType: $errorMsg")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DerivWebSocketService", "Error parsing JSON: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnecting = false
                isConnected = false
                if (activeToken.isNotEmpty()) {
                    _authState.value = AuthState.FAILED
                }
                Log.e("DerivWebSocketService", "WebSocket connection failed: ${t.message}. Starting simulator fallback.")
                startSimulation(activeSymbol)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("DerivWebSocketService", "WebSocket Closed: $reason")
            }
        })
    }

    private fun subscribeToTicks(ws: WebSocket, symbol: String) {
        val payload = JSONObject().apply {
            put("ticks", symbol)
        }
        Log.d("DerivWebSocketService", "Subscribing to ticks for: $symbol")
        ws.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
        isConnected = false
        _authState.value = AuthState.IDLE
        stopSimulation()
    }

    private fun startSimulation(symbol: String) {
        stopSimulation()
        simulationJob = scope.launch {
            val base = basePrices[symbol] ?: 100.0
            val pip = pipSizes[symbol] ?: 2
            currentPrice = base
            
            while (true) {
                val volatility = when (symbol) {
                    "R_10" -> 1.2
                    "R_100" -> 4.5
                    "frxEURUSD" -> 0.00012
                    "cryBTCUSD" -> 35.0
                    "frxXAUUSD" -> 0.65
                    else -> 0.1
                }

                val changePercent = Random.nextDouble(-1.0, 1.0)
                val change = changePercent * volatility
                currentPrice += change

                val multiplier = Math.pow(10.0, pip.toDouble())
                currentPrice = Math.round(currentPrice * multiplier) / multiplier

                val bid = currentPrice - (volatility * 0.05)
                val ask = currentPrice + (volatility * 0.05)

                _tickFlow.emit(
                    TickData(
                        symbol = symbol,
                        quote = currentPrice,
                        bid = Math.round(bid * multiplier) / multiplier,
                        ask = Math.round(ask * multiplier) / multiplier
                    )
                )

                delay(1000)
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun getSymbolLabel(symbol: String): String {
        return when (symbol) {
            "R_10" -> "Volatility 10 (1s) Index"
            "R_100" -> "Volatility 100 (1s) Index"
            "frxEURUSD" -> "EUR/USD Forex"
            "cryBTCUSD" -> "Bitcoin / USD"
            "frxXAUUSD" -> "Gold / USD"
            else -> symbol
        }
    }
}
