package com.example.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.create
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AutoStartApp
import com.example.data.repository.AutoStartRepository
import com.example.data.repository.InstalledAppInfo
import com.example.service.AutoStartService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AutoStartProgressState(
    val isRunning: Boolean = false,
    val statusText: String = "",
    val currentApp: String = "",
    val progressPercent: Int = 0
)

data class PowerState(
    val isPowerConnected: Boolean = true,
    val batteryPercent: Int = 100
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AutoStartRepository(application, db.autoStartDao())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _progressState = MutableStateFlow(AutoStartProgressState())
    val progressState: StateFlow<AutoStartProgressState> = _progressState.asStateFlow()

    private val _powerState = MutableStateFlow(PowerState())
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    val enabledAutoStartApps: StateFlow<List<AutoStartApp>> = repository.enabledAutoStartApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps: StateFlow<List<AutoStartApp>> = repository.favoriteApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    private val _globalDelay = MutableStateFlow(3)
    val globalDelay: StateFlow<Int> = _globalDelay.asStateFlow()

    private val _masterAutoStartEnabled = MutableStateFlow(true)
    val masterAutoStartEnabled: StateFlow<Boolean> = _masterAutoStartEnabled.asStateFlow()

    private val _triggerOnBoot = MutableStateFlow(true)
    val triggerOnBoot: StateFlow<Boolean> = _triggerOnBoot.asStateFlow()

    private val _triggerOnPower = MutableStateFlow(true)
    val triggerOnPower: StateFlow<Boolean> = _triggerOnPower.asStateFlow()

    private val _triggerOnScreen = MutableStateFlow(true)
    val triggerOnScreen: StateFlow<Boolean> = _triggerOnScreen.asStateFlow()

    val filteredInstalledApps: StateFlow<List<InstalledAppInfo>> = combine(
        _installedApps,
        _searchQuery
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AutoStartService.ACTION_AUTOSTART_PROGRESS) {
                val status = intent.getStringExtra(AutoStartService.EXTRA_STATUS) ?: ""
                val currentApp = intent.getStringExtra(AutoStartService.EXTRA_CURRENT_APP) ?: ""
                val progress = intent.getIntExtra(AutoStartService.EXTRA_PROGRESS, 0)
                val isDone = intent.getBooleanExtra(AutoStartService.EXTRA_IS_DONE, false)

                _progressState.value = AutoStartProgressState(
                    isRunning = !isDone,
                    statusText = status,
                    currentApp = currentApp,
                    progressPercent = progress
                )
            }
        }
    }

    init {
        loadSettings()
        refreshInstalledApps()
        checkPowerState()
        checkIfDefaultLauncher()
        registerProgressReceiver()
        registerPowerAndScreenReceiver()
    }

    private var hasAutoStartedOnLaunch = false

    private val powerAndScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            checkPowerState()
            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (_masterAutoStartEnabled.value && _triggerOnPower.value) {
                        startAutoStartSequenceNow()
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (_masterAutoStartEnabled.value && _triggerOnScreen.value) {
                        startAutoStartSequenceNow()
                    }
                }
            }
        }
    }

    private fun registerPowerAndScreenReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getApplication<Application>().registerReceiver(
                    powerAndScreenReceiver, filter, Context.RECEIVER_EXPORTED
                )
            } else {
                getApplication<Application>().registerReceiver(powerAndScreenReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAutoStartOnColdStart() {
        if (!hasAutoStartedOnLaunch) {
            hasAutoStartedOnLaunch = true
            viewModelScope.launch {
                // Short pause to ensure state is initialized
                kotlinx.coroutines.delay(1000)
                if (_masterAutoStartEnabled.value) {
                    startAutoStartSequenceNow()
                }
            }
        }
    }

    fun launchStockLauncher(): Boolean {
        val stockPackage = "com.tc.launcher2"
        val success = repository.launchApp(stockPackage)
        if (!success) {
            openHomeSettings()
        }
        return success
    }

    private fun registerProgressReceiver() {
        try {
            val filter = IntentFilter(AutoStartService.ACTION_AUTOSTART_PROGRESS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getApplication<Application>().registerReceiver(
                    progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                getApplication<Application>().registerReceiver(progressReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _globalDelay.value = repository.getSetting("global_delay", "3").toIntOrNull() ?: 3
            _masterAutoStartEnabled.value = repository.getSetting("autostart_enabled", "true").toBoolean()
            _triggerOnBoot.value = repository.getSetting("trigger_boot", "true").toBoolean()
            _triggerOnPower.value = repository.getSetting("trigger_power", "true").toBoolean()
            _triggerOnScreen.value = repository.getSetting("trigger_screen", "true").toBoolean()
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAutoStart(appInfo: InstalledAppInfo, enable: Boolean) {
        viewModelScope.launch {
            repository.toggleAutoStart(appInfo.packageName, appInfo.appName, enable)
            refreshInstalledApps()
        }
    }

    fun updateAppDelay(packageName: String, delaySec: Int) {
        viewModelScope.launch {
            repository.updateAppDelay(packageName, delaySec)
            refreshInstalledApps()
        }
    }

    fun updateAppOrder(packageName: String, order: Int) {
        viewModelScope.launch {
            repository.updateAppOrder(packageName, order)
            refreshInstalledApps()
        }
    }

    fun toggleFavorite(appInfo: InstalledAppInfo, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(appInfo.packageName, appInfo.appName, isFav)
            refreshInstalledApps()
        }
    }

    fun launchApp(packageName: String): Boolean {
        return repository.launchApp(packageName)
    }

    fun startAutoStartSequenceNow() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, AutoStartService::class.java).apply {
            action = AutoStartService.ACTION_START_AUTOSTART
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                context.startService(serviceIntent)
            }
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopAutoStartSequence() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, AutoStartService::class.java).apply {
            action = AutoStartService.ACTION_STOP_AUTOSTART
        }
        context.startService(serviceIntent)
        _progressState.value = AutoStartProgressState(isRunning = false, statusText = "Отменено")
    }

    fun setMasterAutoStartEnabled(enabled: Boolean) {
        _masterAutoStartEnabled.value = enabled
        viewModelScope.launch {
            repository.setSetting("autostart_enabled", enabled.toString())
        }
    }

    fun setGlobalDelay(delaySec: Int) {
        _globalDelay.value = delaySec
        viewModelScope.launch {
            repository.setSetting("global_delay", delaySec.toString())
        }
    }

    fun setTriggerOnBoot(enabled: Boolean) {
        _triggerOnBoot.value = enabled
        viewModelScope.launch { repository.setSetting("trigger_boot", enabled.toString()) }
    }

    fun setTriggerOnPower(enabled: Boolean) {
        _triggerOnPower.value = enabled
        viewModelScope.launch { repository.setSetting("trigger_power", enabled.toString()) }
    }

    fun setTriggerOnScreen(enabled: Boolean) {
        _triggerOnScreen.value = enabled
        viewModelScope.launch { repository.setSetting("trigger_screen", enabled.toString()) }
    }

    fun openHomeSettings() {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(intent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    fun openBatteryOptimizationSettings() {
        val context = getApplication<Application>()
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                openHomeSettings()
            }
        }
    }

    private fun checkPowerState() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = getApplication<Application>().registerReceiver(null, filter)

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

            _powerState.value = PowerState(isPowerConnected = isCharging, batteryPercent = batteryPct)
        } catch (e: Exception) {
            _powerState.value = PowerState(isPowerConnected = true, batteryPercent = 100)
        }
    }

    fun checkIfDefaultLauncher() {
        try {
            val context = getApplication<Application>()
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            val currentHomePackage = resolveInfo?.activityInfo?.packageName
            _isDefaultLauncher.value = (currentHomePackage == context.packageName)
        } catch (e: Exception) {
            _isDefaultLauncher.value = false
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(progressReceiver)
            getApplication<Application>().unregisterReceiver(powerAndScreenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onCleared()
    }
}
