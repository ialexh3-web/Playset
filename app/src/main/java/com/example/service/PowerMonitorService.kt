package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.utils.WakeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PowerMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isReceiverRegistered = false

    companion object {
        const val MONITOR_CHANNEL_ID = "ottocast_power_monitor_channel"
        const val MONITOR_NOTIFICATION_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, PowerMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    private val powerAndScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return

            val pendingResult = goAsync()
            serviceScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.autoStartDao()

                    val isAutoStartEnabled = dao.getSetting("autostart_enabled")?.toBooleanStrictOrNull() ?: true
                    if (!isAutoStartEnabled) return@launch

                    val triggerOnPower = dao.getSetting("trigger_power")?.toBooleanStrictOrNull() ?: true
                    val triggerOnScreen = dao.getSetting("trigger_screen")?.toBooleanStrictOrNull() ?: true

                    val isPowerEvent = when (action) {
                        Intent.ACTION_POWER_CONNECTED,
                        "android.intent.action.ACTION_POWER_CONNECTED",
                        "android.intent.action.POWER_CONNECTED",
                        "android.intent.action.ACC_ON",
                        "com.microntek.acc.on",
                        "com.microntek.ir.poweron",
                        "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> true
                        else -> false
                    }

                    val isScreenEvent = when (action) {
                        Intent.ACTION_SCREEN_ON,
                        Intent.ACTION_USER_PRESENT -> true
                        else -> false
                    }

                    val shouldTrigger = (isPowerEvent && triggerOnPower) || (isScreenEvent && triggerOnScreen)

                    if (shouldTrigger) {
                        // Wake up screen and launch Activity
                        WakeUtils.wakeUpDeviceAndLaunchActivity(context)

                        // Trigger AutoStartService sequence
                        val serviceIntent = Intent(context, AutoStartService::class.java).apply {
                            this.action = AutoStartService.ACTION_START_AUTOSTART
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
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundMonitorNotification()
        registerPowerAndScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundMonitorNotification()
        if (!isReceiverRegistered) {
            registerPowerAndScreenReceiver()
        }
        return START_STICKY
    }

    private fun registerPowerAndScreenReceiver() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction("android.intent.action.ACTION_POWER_CONNECTED")
                addAction("android.intent.action.POWER_CONNECTED")
                addAction("android.intent.action.ACC_ON")
                addAction("com.microntek.acc.on")
                addAction("com.microntek.ir.poweron")
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
                addAction("android.hardware.usb.action.USB_STATE")
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(powerAndScreenReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(powerAndScreenReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startForegroundMonitorNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setContentTitle("OttoLauncher AI — Мониторинг питания")
            .setContentText("Отслеживание подключения питания и включения экрана")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(MONITOR_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (e: Exception) {
                startForeground(MONITOR_NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Монитор питания и экрана Ottocast",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый мониторинг питания для немедленного автозапуска"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(powerAndScreenReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isReceiverRegistered = false
        }
        super.onDestroy()
    }
}
