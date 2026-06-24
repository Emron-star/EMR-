package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_settings")
data class BotSettings(
    @PrimaryKey val id: Int = 1, // Only 1 active configuration row
    val selectedSymbol: String = "R_10", // Volatility 10 (1s) Index
    val selectedStrategy: String = "SMA_CROSS", // SMA_CROSS, RSI_OS, MACD_CROSS, BOLL_BAND, GEMINI_AI
    val stake: Double = 10.0,
    val takeProfit: Double = 50.0,
    val stopLoss: Double = 30.0,
    val runMode: String = "DEMO", // "DEMO" or "REAL"
    val isRunning: Boolean = false,
    val martingaleMultiplier: Double = 2.0,
    val useMartingale: Boolean = false,
    val digitPrediction: Int = 5
)
