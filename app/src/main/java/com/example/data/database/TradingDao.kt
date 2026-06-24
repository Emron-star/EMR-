package com.example.data.database

import androidx.room.*
import com.example.data.model.ApiCredentials
import com.example.data.model.BotSettings
import com.example.data.model.TradeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {

    // --- Trade Logs ---
    @Query("SELECT * FROM trade_logs ORDER BY timestamp DESC")
    fun getAllTradeLogs(): Flow<List<TradeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeLog(tradeLog: TradeLog)

    @Query("DELETE FROM trade_logs")
    suspend fun clearAllTradeLogs()

    // --- Bot Settings ---
    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    fun getBotSettingsFlow(): Flow<BotSettings?>

    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    suspend fun getBotSettings(): BotSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBotSettings(settings: BotSettings)

    // --- API Credentials ---
    @Query("SELECT * FROM api_credentials WHERE id = 1 LIMIT 1")
    fun getApiCredentialsFlow(): Flow<ApiCredentials?>

    @Query("SELECT * FROM api_credentials WHERE id = 1 LIMIT 1")
    suspend fun getApiCredentials(): ApiCredentials?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApiCredentials(credentials: ApiCredentials)
}
