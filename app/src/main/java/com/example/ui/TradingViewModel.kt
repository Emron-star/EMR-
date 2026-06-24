package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.TradingDatabase
import com.example.data.model.ApiCredentials
import com.example.data.model.BotSettings
import com.example.data.model.TradeLog
import com.example.data.repository.TradingRepository
import com.example.data.network.AiSignal
import com.example.data.network.DerivWebSocketService
import com.example.data.network.GeminiManager
import com.example.data.network.TickData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveContract(
    val contractId: String,
    val symbol: String,
    val strategy: String,
    val tradeType: String, // "RISE" or "FALL"
    val entryPrice: Double,
    val currentPrice: Double,
    val stake: Double,
    val ticksRemaining: Int,
    val maxTicks: Int = 6,
    val tickHistory: List<Double> = emptyList(),
    val targetDigit: Int? = null
)

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradingRepository
    private val webSocketService = DerivWebSocketService()
    private val geminiManager = GeminiManager()

    fun getLastDigit(price: Double, symbol: String): Int {
        val pip = when (symbol) {
            "R_10" -> 2
            "R_100" -> 2
            "frxEURUSD" -> 5
            "cryBTCUSD" -> 2
            "frxXAUUSD" -> 2
            else -> 2
        }
        val formatted = String.format(java.util.Locale.US, "%.${pip}f", price)
        val lastChar = formatted.lastOrNull { it.isDigit() }
        return lastChar?.toString()?.toIntOrNull() ?: 0
    }

    // --- State Flows ---
    val botSettings: StateFlow<BotSettings>
    val apiCredentials: StateFlow<ApiCredentials>
    val tradeLogs: StateFlow<List<TradeLog>>

    private val _tickHistory = MutableStateFlow<List<TickData>>(emptyList())
    val tickHistory: StateFlow<List<TickData>> = _tickHistory.asStateFlow()

    private val _activeContract = MutableStateFlow<ActiveContract?>(null)
    val activeContract: StateFlow<ActiveContract?> = _activeContract.asStateFlow()

    private val _isAnalyzingMarket = MutableStateFlow(false)
    val isAnalyzingMarket: StateFlow<Boolean> = _isAnalyzingMarket.asStateFlow()

    private val _marketAnalysisText = MutableStateFlow("Select a symbol to trigger dynamic AI commentary and technical strategy advice.")
    val marketAnalysisText: StateFlow<String> = _marketAnalysisText.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("AI", "Welcome to NexusTrader. I am your Gemini-powered quantitative assistant. Ask me anything about strategies, risk structures, or current market behaviors."))
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _isAILookingForSignal = MutableStateFlow(false)
    val isAILookingForSignal: StateFlow<Boolean> = _isAILookingForSignal.asStateFlow()

    // Base current stake (changes based on Martingale multiplier)
    private val _currentStake = MutableStateFlow(10.0)
    val currentStake: StateFlow<Double> = _currentStake.asStateFlow()

    // Demo Initial Balance
    private val startingBalance = 10000.0
    val totalBalance: StateFlow<Double>

    private var tickCollectionJob: Job? = null
    private var isGeminiCallInFlight = false

    init {
        val database = TradingDatabase.getDatabase(application)
        repository = TradingRepository(database.tradingDao())

        botSettings = repository.botSettingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BotSettings()
        )

        apiCredentials = repository.apiCredentialsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApiCredentials()
        )

        tradeLogs = repository.allTradeLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Dynamically compute total balance based on completed trade profits
        totalBalance = tradeLogs.map { logs ->
            startingBalance + logs.sumOf { it.profit }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = startingBalance
        )

        // Set initial stake based on current configuration
        viewModelScope.launch {
            botSettings.collect { settings ->
                _currentStake.value = settings.stake
            }
        }

        // Auto connect to the default symbol
        connectToSymbol("R_10")
    }

    fun connectToSymbol(symbol: String) {
        tickCollectionJob?.cancel()
        _tickHistory.value = emptyList()

        // Update database active symbol
        viewModelScope.launch {
            val current = repository.getBotSettings()
            if (current.selectedSymbol != symbol) {
                repository.updateBotSettings(current.copy(selectedSymbol = symbol))
            }
        }

        val creds = apiCredentials.value
        webSocketService.connect(creds.appId, creds.apiToken, symbol)

        tickCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            webSocketService.tickFlow.collect { tick ->
                // Maintain max 40 ticks in local state for chart rendering
                _tickHistory.update { history ->
                    val nextList = history.toMutableList()
                    nextList.add(tick)
                    if (nextList.size > 40) {
                        nextList.removeAt(0)
                    }
                    nextList
                }

                // Feed the tick to the contract in-play or the trading bots
                processIncomingTick(tick)
            }
        }
    }

    private suspend fun processIncomingTick(tick: TickData) {
        val contract = _activeContract.value
        val settings = repository.getBotSettings()

        if (contract != null) {
            // --- We have an active contract in play ---
            val updatedHistory = contract.tickHistory.toMutableList().apply { add(tick.quote) }
            val nextTicksRemaining = contract.ticksRemaining - 1

            if (nextTicksRemaining <= 0) {
                // Settle contract!
                val entryPrice = contract.entryPrice
                val exitPrice = tick.quote
                val tradeType = contract.tradeType
                val stake = contract.stake

                val lastDigit = getLastDigit(exitPrice, contract.symbol)
                val target = contract.targetDigit ?: settings.digitPrediction

                val isWin = when (tradeType) {
                    "RISE" -> exitPrice > entryPrice
                    "FALL" -> exitPrice < entryPrice
                    "MATCH" -> lastDigit == target
                    "DIFF" -> lastDigit != target
                    "EVEN" -> lastDigit % 2 == 0
                    "ODD" -> lastDigit % 2 != 0
                    "OVER" -> lastDigit > target
                    "UNDER" -> lastDigit < target
                    else -> exitPrice > entryPrice
                }

                val payoutPercent = when (tradeType) {
                    "MATCH" -> 8.50  // 850% payout return
                    "DIFF" -> 0.09   // 9% payout return
                    "EVEN", "ODD" -> 0.95  // 95% payout return
                    "OVER", "UNDER" -> {
                        val winningDigits = if (tradeType == "OVER") (9 - target) else target
                        if (winningDigits in 1..9) {
                            (10.0 - winningDigits) / winningDigits * 0.9
                        } else {
                            0.95
                        }
                    }
                    else -> 0.85 // 85% payout return
                }

                val profit = if (isWin) {
                    stake * payoutPercent
                } else {
                    -stake
                }

                val payout = if (isWin) stake + (stake * payoutPercent) else 0.0
                val status = if (isWin) "WON" else "LOST"

                val log = TradeLog(
                    contractId = contract.contractId,
                    symbol = contract.symbol,
                    strategy = contract.strategy,
                    tradeType = tradeType,
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    stake = stake,
                    payout = payout,
                    profit = profit,
                    status = status
                )

                repository.insertTradeLog(log)
                _activeContract.value = null

                Log.d("TradingBot", "Contract Settled! Result: $status. Profit: $profit")

                // Handle Martingale adjustment
                if (settings.useMartingale) {
                    if (isWin) {
                        // Reset stake to original base
                        _currentStake.value = settings.stake
                    } else {
                        // Multiply stake
                        _currentStake.value = stake * settings.martingaleMultiplier
                    }
                } else {
                    _currentStake.value = settings.stake
                }

            } else {
                // Update active contract state
                _activeContract.value = contract.copy(
                    currentPrice = tick.quote,
                    ticksRemaining = nextTicksRemaining,
                    tickHistory = updatedHistory
                )
            }

        } else if (settings.isRunning) {
            // --- No active contract and Bot is running -> Look for signals ---
            val tickValues = _tickHistory.value.map { it.quote }
            val isDigitStrategy = settings.selectedStrategy.startsWith("DIGIT_")

            if (!isDigitStrategy && tickValues.size < 15) return // Wait for minimum ticks to build indicators
            if (isDigitStrategy && tickValues.size < 5) return // Wait for minimum ticks for digits

            // Only allow 1 search request at a time
            if (isGeminiCallInFlight) return

            when (settings.selectedStrategy) {
                "SMA_CROSS" -> {
                    val sma3 = calculateSma(tickValues, 3)
                    val sma8 = calculateSma(tickValues, 8)
                    val prevSma3 = calculateSma(tickValues.dropLast(1), 3)
                    val prevSma8 = calculateSma(tickValues.dropLast(1), 8)

                    // Bullish cross: Short crosses above Long
                    if (prevSma3 <= prevSma8 && sma3 > sma8) {
                        executeSimulatedTrade("RISE", "SMA Crossover")
                    }
                    // Bearish cross: Short crosses below Long
                    else if (prevSma3 >= prevSma8 && sma3 < sma8) {
                        executeSimulatedTrade("FALL", "SMA Crossover")
                    }
                }
                "RSI_OS" -> {
                    val rsi = calculateRsi(tickValues, 10)
                    val prevRsi = calculateRsi(tickValues.dropLast(1), 10)

                    // Oversold reversal: RSI crosses above 30
                    if (prevRsi <= 30.0 && rsi > 30.0) {
                        executeSimulatedTrade("RISE", "RSI Oversold")
                    }
                    // Overbought reversal: RSI crosses below 70
                    else if (prevRsi >= 70.0 && rsi < 70.0) {
                        executeSimulatedTrade("FALL", "RSI Overbought")
                    }
                }
                "BOLL_BAND" -> {
                    val lastPrice = tickValues.last()
                    val prevPrice = tickValues[tickValues.size - 2]
                    val bands = calculateBollingerBands(tickValues, 10) // mean, upper, lower

                    // Crossed lower band: potential bounce up
                    if (prevPrice >= bands.third && lastPrice < bands.third) {
                        executeSimulatedTrade("RISE", "Bollinger Breakout")
                    }
                    // Crossed upper band: potential reversal down
                    else if (prevPrice <= bands.second && lastPrice > bands.second) {
                        executeSimulatedTrade("FALL", "Bollinger Breakout")
                    }
                }
                "GEMINI_AI" -> {
                    isGeminiCallInFlight = true
                    _isAILookingForSignal.value = true
                    viewModelScope.launch {
                        try {
                            val symbolLabel = webSocketService.getSymbolLabel(settings.selectedSymbol)
                            val signal = geminiManager.getAiSignal(tickValues.takeLast(15), symbolLabel)
                            
                            if (settings.isRunning && _activeContract.value == null) {
                                if (signal.action == "RISE") {
                                    executeSimulatedTrade("RISE", "Gemini AI (${String.format("%.0f%%", signal.confidence * 100)})")
                                } else if (signal.action == "FALL") {
                                    executeSimulatedTrade("FALL", "Gemini AI (${String.format("%.0f%%", signal.confidence * 100)})")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TradingBot", "Error getting Gemini Signal: ${e.message}")
                        } finally {
                            isGeminiCallInFlight = false
                            _isAILookingForSignal.value = false
                        }
                    }
                }
                "DIGIT_MATCH" -> {
                    val digitsList = tickValues.map { getLastDigit(it, settings.selectedSymbol) }
                    val last5 = digitsList.takeLast(5)
                    if (last5.size >= 5 && last5.none { it == settings.digitPrediction }) {
                        executeSimulatedTrade(
                            tradeType = "MATCH",
                            strategyLabel = "Digit Matches (Target: ${settings.digitPrediction})",
                            maxTicks = 1,
                            targetDigit = settings.digitPrediction
                        )
                    }
                }
                "DIGIT_DIFF" -> {
                    val digitsList = tickValues.map { getLastDigit(it, settings.selectedSymbol) }
                    val lastDigit = digitsList.lastOrNull() ?: 0
                    if (lastDigit == settings.digitPrediction) {
                        executeSimulatedTrade(
                            tradeType = "DIFF",
                            strategyLabel = "Digit Differs (Target: ${settings.digitPrediction})",
                            maxTicks = 1,
                            targetDigit = settings.digitPrediction
                        )
                    }
                }
                "DIGIT_EVEN" -> {
                    val digitsList = tickValues.map { getLastDigit(it, settings.selectedSymbol) }
                    val last3 = digitsList.takeLast(3)
                    if (last3.size >= 3 && last3.all { it % 2 != 0 }) {
                        executeSimulatedTrade(
                            tradeType = "EVEN",
                            strategyLabel = "Digit Even Reversal",
                            maxTicks = 1
                        )
                    }
                }
                "DIGIT_ODD" -> {
                    val digitsList = tickValues.map { getLastDigit(it, settings.selectedSymbol) }
                    val last3 = digitsList.takeLast(3)
                    if (last3.size >= 3 && last3.all { it % 2 == 0 }) {
                        executeSimulatedTrade(
                            tradeType = "ODD",
                            strategyLabel = "Digit Odd Reversal",
                            maxTicks = 1
                        )
                    }
                }
                "DIGIT_OVER" -> {
                    val digitsList = tickValues.map { getLastDigit(it, settings.selectedSymbol) }
                    val last3 = digitsList.takeLast(3)
                    if (last3.size >= 3 && last3.all { it <= settings.digitPrediction }) {
                        executeSimulatedTrade(
                            tradeType = "OVER",
                            strategyLabel = "Digit Over (Barrier: ${settings.digitPrediction})",
                            maxTicks = 1,
                            targetDigit = settings.digitPrediction
                        )
                    }
                }
                "DIGIT_UNDER" -> {
                    val digitsList = tickValues.map { getLastDigit(it, settings.selectedSymbol) }
                    val last3 = digitsList.takeLast(3)
                    if (last3.size >= 3 && last3.all { it >= settings.digitPrediction }) {
                        executeSimulatedTrade(
                            tradeType = "UNDER",
                            strategyLabel = "Digit Under (Barrier: ${settings.digitPrediction})",
                            maxTicks = 1,
                            targetDigit = settings.digitPrediction
                        )
                    }
                }
            }
        }
    }

    private fun executeSimulatedTrade(
        tradeType: String, 
        strategyLabel: String, 
        maxTicks: Int = 6, 
        targetDigit: Int? = null
    ) {
        val currentHistory = _tickHistory.value
        if (currentHistory.isEmpty()) return

        val entryPrice = currentHistory.last().quote
        val contractId = "CNT_" + UUID.randomUUID().toString().substring(0, 8).uppercase()

        _activeContract.value = ActiveContract(
            contractId = contractId,
            symbol = activeSymbol.value,
            strategy = strategyLabel,
            tradeType = tradeType,
            entryPrice = entryPrice,
            currentPrice = entryPrice,
            stake = _currentStake.value,
            ticksRemaining = maxTicks,
            maxTicks = maxTicks,
            tickHistory = listOf(entryPrice),
            targetDigit = targetDigit
        )

        Log.d("TradingBot", "Order Executed: Buy $tradeType on $activeSymbol. Entry: $entryPrice, Stake: ${_currentStake.value}")
    }

    // --- Dynamic UI Settings Handlers ---
    val activeSymbol: StateFlow<String> = botSettings.map { it.selectedSymbol }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "R_10"
    )

    fun updateSettings(
        strategy: String,
        stake: Double,
        takeProfit: Double,
        stopLoss: Double,
        useMartingale: Boolean,
        martingaleMultiplier: Double,
        digitPrediction: Int = 5
    ) {
        viewModelScope.launch {
            val current = repository.getBotSettings()
            val updated = current.copy(
                selectedStrategy = strategy,
                stake = stake,
                takeProfit = takeProfit,
                stopLoss = stopLoss,
                useMartingale = useMartingale,
                martingaleMultiplier = martingaleMultiplier,
                digitPrediction = digitPrediction
            )
            repository.updateBotSettings(updated)
            // Sync current stake
            if (!updated.isRunning) {
                _currentStake.value = stake
            }
        }
    }

    fun toggleBot() {
        viewModelScope.launch {
            val current = repository.getBotSettings()
            val nextState = !current.isRunning
            
            // If starting bot, reset the current stake to original base stake
            if (nextState) {
                _currentStake.value = current.stake
            }

            repository.updateBotSettings(current.copy(isRunning = nextState))
            Log.d("TradingBot", "Bot execution status updated: $nextState")
        }
    }

    fun updateApiCredentials(appId: String, token: String) {
        viewModelScope.launch {
            val creds = ApiCredentials(appId = appId, apiToken = token)
            repository.updateApiCredentials(creds)
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearAllTradeLogs()
            // Reset stake
            val settings = repository.getBotSettings()
            _currentStake.value = settings.stake
        }
    }

    // --- AI Assistant Interactivity ---
    fun fetchMarketAnalysis() {
        viewModelScope.launch {
            _isAnalyzingMarket.value = true
            val ticks = _tickHistory.value.map { it.quote }
            val label = webSocketService.getSymbolLabel(botSettings.value.selectedSymbol)
            try {
                val analysis = geminiManager.analyzeMarket(ticks, label)
                _marketAnalysisText.value = analysis
            } catch (e: Exception) {
                _marketAnalysisText.value = "Unable to retrieve AI analysis. Verify internet connection."
            } finally {
                _isAnalyzingMarket.value = false
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(ChatMessage("USER", text))
        _chatMessages.value = currentMessages

        _isAiThinking.value = true
        viewModelScope.launch {
            val contextTicks = _tickHistory.value.map { it.quote }
            val symbolLabel = webSocketService.getSymbolLabel(botSettings.value.selectedSymbol)
            val responseText = try {
                val prompt = """
                    User asks: "$text".
                    For current context, the user is looking at the $symbolLabel market. Recent prices are: ${contextTicks.takeLast(10).joinToString(", ")}.
                    Answer their question in an incredibly informative, professional, and friendly way. Keep your answer under 3 compact paragraphs, focusing on automated trading strategies, quant indicators, and risk parameters. Use bullet points if listing ideas.
                """.trimIndent()
                geminiManager.analyzeMarket(contextTicks, prompt)
            } catch (e: Exception) {
                "I'm sorry, I encountered an issue querying the market matrix. Please try again."
            }
            
            _chatMessages.update { messages ->
                messages + ChatMessage("AI", responseText)
            }
            _isAiThinking.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketService.disconnect()
        tickCollectionJob?.cancel()
    }

    // --- Helper Technical Indicators Math ---
    private fun calculateSma(ticks: List<Double>, period: Int): Double {
        if (ticks.size < period) return 0.0
        return ticks.takeLast(period).average()
    }

    private fun calculateRsi(ticks: List<Double>, period: Int): Double {
        if (ticks.size < period + 1) return 50.0
        var gains = 0.0
        var losses = 0.0
        for (i in (ticks.size - period) until ticks.size) {
            val change = ticks[i] - ticks[i - 1]
            if (change > 0) {
                gains += change
            } else {
                losses -= change
            }
        }
        if (losses == 0.0) return 100.0
        val rs = gains / losses
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun calculateBollingerBands(ticks: List<Double>, period: Int): Triple<Double, Double, Double> {
        if (ticks.size < period) return Triple(0.0, 0.0, 0.0)
        val sub = ticks.takeLast(period)
        val mean = sub.average()
        val variance = sub.sumOf { Math.pow(it - mean, 2.0) } / period
        val sd = Math.sqrt(variance)
        return Triple(mean, mean + (2 * sd), mean - (2 * sd))
    }

    fun getSymbolLabel(symbol: String) = webSocketService.getSymbolLabel(symbol)
}
