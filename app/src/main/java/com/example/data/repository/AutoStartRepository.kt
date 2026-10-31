package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.data.dao.AutoStartDao
import com.example.data.model.AppSetting
import com.example.data.model.AutoStartApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val autoStartConfig: AutoStartApp? = null
)

class AutoStartRepository(
    private val context: Context,
    private val dao: AutoStartDao
) {
    val allAutoStartApps: Flow<List<AutoStartApp>> = dao.getAllAutoStartApps()
    val enabledAutoStartApps: Flow<List<AutoStartApp>> = dao.getEnabledAutoStartApps()
    val favoriteApps: Flow<List<AutoStartApp>> = dao.getFavoriteApps()

    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED)
        val currentPackageName = context.packageName

        val enabledMap = dao.getEnabledAutoStartAppsSync().associateBy { it.packageName }

        resolveInfos
            .filter { it.activityInfo.packageName != currentPackageName }
            .map { resolveInfo ->
                val pName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = try { resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
                val savedConfig = dao.getAppByPackage(pName) ?: enabledMap[pName]

                InstalledAppInfo(
                    packageName = pName,
                    appName = label,
                    icon = icon,
                    autoStartConfig = savedConfig
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    suspend fun getEnabledAutoStartAppsSync(): List<AutoStartApp> = withContext(Dispatchers.IO) {
        dao.getEnabledAutoStartAppsSync()
    }

    suspend fun updateAppConfig(app: AutoStartApp) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateApp(app)
    }

    suspend fun toggleAutoStart(packageName: String, appName: String, enable: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getAppByPackage(packageName)
        if (existing != null) {
            dao.setAppEnabled(packageName, enable)
        } else if (enable) {
            val count = dao.getEnabledAutoStartAppsSync().size
            dao.insertOrUpdateApp(
                AutoStartApp(
                    packageName = packageName,
                    appName = appName,
                    launchOrder = count + 1,
                    delaySeconds = 3,
                    isEnabled = true
                )
            )
        }
    }

    suspend fun updateAppDelay(packageName: String, delaySeconds: Int) = withContext(Dispatchers.IO) {
        dao.setAppDelay(packageName, delaySeconds)
    }

    suspend fun updateAppOrder(packageName: String, order: Int) = withContext(Dispatchers.IO) {
        dao.setAppOrder(packageName, order)
    }

    suspend fun toggleFavorite(packageName: String, appName: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getAppByPackage(packageName)
        if (existing != null) {
            dao.setAppFavorite(packageName, isFavorite)
        } else {
            dao.insertOrUpdateApp(
                AutoStartApp(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = false,
                    isFavorite = isFavorite
                )
            )
        }
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        dao.setSetting(AppSetting(key, value))
    }

    suspend fun getSetting(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        dao.getSetting(key) ?: defaultValue
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
