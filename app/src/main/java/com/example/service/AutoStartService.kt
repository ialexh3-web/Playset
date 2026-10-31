package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.repository.AutoStartRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoStartService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "ottocast_autostart_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_AUTOSTART = "com.example.action.START_AUTOSTART"
        const val ACTION_STOP_AUTOSTART = "com.example.action.STOP_AUTOSTART"

        const val ACTION_AUTOSTART_PROGRESS = "com.example.action.AUTOSTART_PROGRESS"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_CURRENT_APP = "extra_current_app"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_IS_DONE = "extra_is_done"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_AUTOSTART

        if (action == ACTION_STOP_AUTOSTART) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            startForegroundServiceNotification("Запуск авто-старта приложений...")
            acquireWakeLock()
            runAutoStartSequence()
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "OttoLauncher:AutoStartWakeLock"
            ).apply {
                acquire(45000) // 45s timeout max
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bringMainActivityToFront() {
        try {
            val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(mainIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runAutoStartSequence() {
        bringMainActivityToFront()
        serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = AutoStartRepository(applicationContext, database.autoStartDao())

            val enabledApps = repository.getEnabledAutoStartAppsSync()
            if (enabledApps.isEmpty()) {
                sendProgressBroadcast("Нет приложений для автозапуска", "", 100, true)
                stopSelf()
                return@launch
            }

            val globalDelayStr = repository.getSetting("global_delay", "3")
            val globalDelaySec = globalDelayStr.toIntOrNull() ?: 3

            // Initial startup countdown
            for (sec in globalDelaySec downTo 1) {
                updateNotification("Ожидание запуска: $sec сек...")
                sendProgressBroadcast("Запуск через $sec сек...", "", ((globalDelaySec - sec) * 100) / globalDelaySec, false)
                delay(1000)
            }

            // Launch sequence
            val totalApps = enabledApps.size
            enabledApps.forEachIndexed { index, app ->
                val appDelay = app.delaySeconds.coerceAtLeast(1)
                for (sec in appDelay downTo 1) {
                    val statusText = "Запуск ${app.appName} через $sec сек..."
                    updateNotification(statusText)
                    sendProgressBroadcast(statusText, app.appName, ((index) * 100) / totalApps, false)
                    delay(1000)
                }

                updateNotification("Запускаем ${app.appName}...")
                sendProgressBroadcast("Запуск ${app.appName}...", app.appName, ((index + 1) * 100) / totalApps, false)
                
                val launched = repository.launchApp(app.packageName)
                if (launched) {
                    repository.updateAppConfig(app.copy(
                        launchCount = app.launchCount + 1,
                        lastLaunchedTimestamp = System.currentTimeMillis()
                    ))
                }
                delay(1500)
            }

            sendProgressBroadcast("Автозапуск завершен!", "", 100, true)
            releaseWakeLock()
            stopSelf()
        }
    }

    private fun sendProgressBroadcast(status: String, appName: String, progress: Int, isDone: Boolean) {
        val intent = Intent(ACTION_AUTOSTART_PROGRESS).apply {
            putExtra(EXTRA_STATUS, status)
            putExtra(EXTRA_CURRENT_APP, appName)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_IS_DONE, isDone)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun startForegroundServiceNotification(content: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OttoLauncher AI")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OttoLauncher AI")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ottocast AutoStart Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновая служба автозапуска приложений"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        releaseWakeLock()
        super.onDestroy()
    }
}
