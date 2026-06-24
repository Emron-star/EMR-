package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_credentials")
data class ApiCredentials(
    @PrimaryKey val id: Int = 1, // Only 1 set of credentials
    val appId: String = "1089", // Deriv public default App ID for testing
    val apiToken: String = "" // User PAT
)
