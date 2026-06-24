package com.example.data.repository

import com.example.data.database.TradingDao
import com.example.data.model.ApiCredentials
import com.example.data.model.BotSettings
import com.example.data.model.TradeLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TradingRepository(private val dao: TradingDao) {

    val allTradeLogs: Flow<List<TradeLog>> = dao.getAllTradeLogs()

    val botSettingsFlow: Flow<BotSettings> = dao.getBotSettingsFlow().map { it ?: BotSettings() }

    val apiCredentialsFlow: Flow<ApiCredentials> = dao.getApiCredentialsFlow().map { it ?: ApiCredentials() }

    suspend fun getBotSettings(): BotSettings {
        return dao.getBotSettings() ?: BotSettings()
    }

    suspend fun updateBotSettings(settings: BotSettings) {
        dao.insertOrUpdateBotSettings(settings)
    }

    suspend fun getApiCredentials(): ApiCredentials {
        return dao.getApiCredentials() ?: ApiCredentials()
    }

    suspend fun updateApiCredentials(credentials: ApiCredentials) {
        dao.insertOrUpdateApiCredentials(credentials)
    }

    suspend fun insertTradeLog(tradeLog: TradeLog) {
        dao.insertTradeLog(tradeLog)
    }

    suspend fun clearAllTradeLogs() {
        dao.clearAllTradeLogs()
    }
}
