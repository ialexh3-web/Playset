package com.example

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.PowerMonitorService
import com.example.ui.components.CountdownWidget
import com.example.ui.components.HUDHeader
import com.example.ui.screens.AutoStartConfigScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OttoLauncherTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_POWER_CONNECTED ||
                action == "android.intent.action.ACTION_POWER_CONNECTED" ||
                action == "android.intent.action.ACC_ON" ||
                action == Intent.ACTION_SCREEN_ON
            ) {
                wakeUpDeviceAndShow()
                viewModel.startAutoStartSequenceNow()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        wakeUpDeviceAndShow()

        // Ensure persistent daemon monitor service for power and screen is active
        PowerMonitorService.start(this)

        setContent {
            OttoLauncherTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction("android.intent.action.ACTION_POWER_CONNECTED")
            addAction("android.intent.action.ACC_ON")
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(powerReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(powerReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        wakeUpDeviceAndShow()
        PowerMonitorService.start(this)
        viewModel.refreshInstalledApps()
        viewModel.checkIfDefaultLauncher()
        viewModel.checkAutoStartOnColdStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        wakeUpDeviceAndShow()
    }

    private fun wakeUpDeviceAndShow() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "OttoLauncher:ActivityWakeLock"
                )
                wakeLock.acquire(5000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val filteredInstalledApps by viewModel.filteredInstalledApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val enabledAutoStartApps by viewModel.enabledAutoStartApps.collectAsStateWithLifecycle()
    val progressState by viewModel.progressState.collectAsStateWithLifecycle()
    val powerState by viewModel.powerState.collectAsStateWithLifecycle()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()

    val masterAutoStartEnabled by viewModel.masterAutoStartEnabled.collectAsStateWithLifecycle()
    val globalDelay by viewModel.globalDelay.collectAsStateWithLifecycle()
    val triggerOnBoot by viewModel.triggerOnBoot.collectAsStateWithLifecycle()
    val triggerOnPower by viewModel.triggerOnPower.collectAsStateWithLifecycle()
    val triggerOnScreen by viewModel.triggerOnScreen.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            HUDHeader(
                isMasterAutoStart = masterAutoStartEnabled,
                isDefaultLauncher = isDefaultLauncher,
                onLaunchStockLauncher = { viewModel.launchStockLauncher() },
                onOpenSettings = { viewModel.openHomeSettings() },
                onSetDefaultLauncher = { viewModel.openHomeSettings() },
                onTriggerAutoStart = { viewModel.startAutoStartSequenceNow() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Real-time Countdown Banner Widget
            CountdownWidget(
                isRunning = progressState.isRunning,
                statusText = progressState.statusText,
                currentApp = progressState.currentApp,
                progressPercent = progressState.progressPercent,
                onCancel = { viewModel.stopAutoStartSequence() },
                onStartNow = { viewModel.startAutoStartSequenceNow() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Direct Auto-Start Application Selection & Configuration
            AutoStartConfigScreen(
                installedApps = filteredInstalledApps,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                onToggleAutoStart = { appInfo, enable -> viewModel.toggleAutoStart(appInfo, enable) },
                onToggleFavorite = { appInfo, fav -> viewModel.toggleFavorite(appInfo, fav) },
                onUpdateDelay = { pName, delaySec -> viewModel.updateAppDelay(pName, delaySec) },
                onUpdateOrder = { pName, order -> viewModel.updateAppOrder(pName, order) },
                onLaunchApp = { packageName -> viewModel.launchApp(packageName) },
                onBack = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
