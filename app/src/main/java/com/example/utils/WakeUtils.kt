package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object WakeUtils {

    const val WAKE_CHANNEL_ID = "ottocast_wake_channel"
    const val WAKE_NOTIFICATION_ID = 2002

    fun wakeUpDeviceAndLaunchActivity(context: Context) {
        // 1. Acquire PowerManager WakeLock to wake screen from sleep/suspended state
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    "OttoLauncher:WakeLock"
                )
                wakeLock.acquire(10000) // Keep screen on for 10 seconds
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Prepare Intent to launch MainActivity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        // 3. Direct Activity Launch
        try {
            context.startActivity(mainIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. High-Priority FullScreenIntent Notification to bypass Android 10+ BAL restrictions
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        WAKE_CHANNEL_ID,
                        "OttoLauncher Waker",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Канал для пробуждения экрана при подключении питания"
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    }
                    nm.createNotificationChannel(channel)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    mainIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, WAKE_CHANNEL_ID)
                    .setContentTitle("OttoLauncher AI")
                    .setContentText("Запуск приложений при подключении питания...")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .build()

                nm.notify(WAKE_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
