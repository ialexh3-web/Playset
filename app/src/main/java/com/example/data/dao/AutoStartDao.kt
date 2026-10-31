package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSetting
import com.example.data.model.AutoStartApp
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoStartDao {
    @Query("SELECT * FROM auto_start_apps ORDER BY launchOrder ASC, appName ASC")
    fun getAllAutoStartApps(): Flow<List<AutoStartApp>>

    @Query("SELECT * FROM auto_start_apps WHERE isEnabled = 1 ORDER BY launchOrder ASC")
    fun getEnabledAutoStartAppsSync(): List<AutoStartApp>

    @Query("SELECT * FROM auto_start_apps WHERE isEnabled = 1 ORDER BY launchOrder ASC")
    fun getEnabledAutoStartApps(): Flow<List<AutoStartApp>>

    @Query("SELECT * FROM auto_start_apps WHERE isFavorite = 1 ORDER BY appName ASC")
    fun getFavoriteApps(): Flow<List<AutoStartApp>>

    @Query("SELECT * FROM auto_start_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): AutoStartApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: AutoStartApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApps(apps: List<AutoStartApp>)

    @Delete
    suspend fun deleteApp(app: AutoStartApp)

    @Query("DELETE FROM auto_start_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE auto_start_apps SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun setAppEnabled(packageName: String, isEnabled: Boolean)

    @Query("UPDATE auto_start_apps SET delaySeconds = :delaySeconds WHERE packageName = :packageName")
    suspend fun setAppDelay(packageName: String, delaySeconds: Int)

    @Query("UPDATE auto_start_apps SET launchOrder = :order WHERE packageName = :packageName")
    suspend fun setAppOrder(packageName: String, order: Int)

    @Query("UPDATE auto_start_apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setAppFavorite(packageName: String, isFavorite: Boolean)

    // Settings key-value pairs
    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)
}
