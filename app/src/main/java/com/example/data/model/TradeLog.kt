package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_logs")
data class TradeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contractId: String,
    val symbol: String,
    val strategy: String,
    val tradeType: String, // "RISE" (BUY) or "FALL" (SELL)
    val entryPrice: Double,
    val exitPrice: Double,
    val stake: Double,
    val payout: Double,
    val profit: Double,
    val status: String, // "WON", "LOST", "PENDING"
    val timestamp: Long = System.currentTimeMillis()
)
