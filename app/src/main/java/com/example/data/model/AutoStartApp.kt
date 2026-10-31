package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_start_apps")
data class AutoStartApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val launchOrder: Int = 1,
    val delaySeconds: Int = 3,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchedTimestamp: Long = 0L
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
