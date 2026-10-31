package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.service.AutoStartService
import com.example.service.PowerMonitorService
import com.example.utils.WakeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        // Immediately launch persistent power & screen monitor daemon service
        PowerMonitorService.start(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.autoStartDao()

                val isAutoStartEnabled = dao.getSetting("autostart_enabled")?.toBooleanStrictOrNull() ?: true
                if (!isAutoStartEnabled) return@launch

                val triggerOnBoot = dao.getSetting("trigger_boot")?.toBooleanStrictOrNull() ?: true
                val triggerOnPower = dao.getSetting("trigger_power")?.toBooleanStrictOrNull() ?: true
                val triggerOnScreen = dao.getSetting("trigger_screen")?.toBooleanStrictOrNull() ?: true

                val shouldTrigger = when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_LOCKED_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON",
                    "com.htc.intent.action.QUICKBOOT_POWERON",
                    "android.intent.action.REBOOT",
                    "android.intent.action.ACC_ON",
                    "com.microntek.acc.on",
                    "com.microntek.ir.poweron",
                    "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> triggerOnBoot

                    Intent.ACTION_POWER_CONNECTED,
                    "android.intent.action.ACTION_POWER_CONNECTED",
                    "android.intent.action.POWER_CONNECTED" -> triggerOnPower

                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_USER_PRESENT -> triggerOnScreen

                    else -> false
                }

                if (shouldTrigger) {
                    // Turn screen on and bring UI to front
                    WakeUtils.wakeUpDeviceAndLaunchActivity(context)

                    // Start background/foreground autostart sequence
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
