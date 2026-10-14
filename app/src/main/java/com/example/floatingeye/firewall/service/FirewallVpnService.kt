package com.example.floatingeye.firewall.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.floatingeye.firewall.repository.FirewallRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FirewallVpnService : VpnService(), Runnable {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    @Volatile
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD_RULES -> {
                reloadVpnRules()
                return START_STICKY
            }
            else -> {
                startForegroundNotification()
                startVpn()
                return START_STICKY
            }
        }
    }

    private fun startForegroundNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "HIRIS Network Firewall",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active protection blocking selected apps from mobile data & Wi-Fi"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_extension_id", "firewall")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FirewallVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val repository = FirewallRepository.getInstance(this)
        val blockedCount = repository.getCurrentlyBlockedPackages().size
        val networkType = repository.activeNetworkType.value.name

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🛡️ HIRIS Firewall Active")
            .setContentText("$blockedCount app(s) isolated from $networkType internet")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Firewall", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Synchronized
    private fun startVpn() {
        if (isRunning) return
        configureVpn()
    }

    @Synchronized
    private fun reloadVpnRules() {
        closeVpnInterface()
        configureVpn()
        startForegroundNotification()
    }

    @Synchronized
    private fun configureVpn() {
        val repository = FirewallRepository.getInstance(this)
        val blockedPackages = repository.getCurrentlyBlockedPackages()

        // If no packages are blocked, we don't need a live TUN interface
        if (blockedPackages.isEmpty()) {
            closeVpnInterface()
            return
        }

        try {
            val builder = Builder()
                .setSession("HIRIS Network Sentinel")
                .setMtu(1500)
                .addAddress("10.120.0.1", 24)
                .addRoute("0.0.0.0", 0)

            var addedCount = 0
            for (pkg in blockedPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                    addedCount++
                } catch (_: Exception) {}
            }

            if (addedCount == 0) {
                closeVpnInterface()
                return
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                workerThread = Thread(this, "HirisFirewallWorker").apply { start() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun run() {
        val descriptor = vpnInterface ?: return
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(32768)

        try {
            // Drain incoming packets from blocked apps to blackhole them cleanly
            while (isRunning && !Thread.interrupted()) {
                val read = inputStream.read(buffer)
                if (read <= 0) break
                // Packet is blackholed (dropped silently without routing)
            }
        } catch (_: IOException) {
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    private fun closeVpnInterface() {
        isRunning = false
        workerThread?.interrupt()
        workerThread = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    @Synchronized
    private fun stopVpn() {
        closeVpnInterface()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        val repository = FirewallRepository.getInstance(this)
        repository.setFirewallActive(false)
        stopVpn()
        super.onRevoke()
    }

    companion object {
        const val ACTION_START = "com.example.floatingeye.firewall.START"
        const val ACTION_STOP = "com.example.floatingeye.firewall.STOP"
        const val ACTION_RELOAD_RULES = "com.example.floatingeye.firewall.RELOAD_RULES"

        private const val NOTIFICATION_CHANNEL_ID = "hiris_firewall_channel"
        private const val NOTIFICATION_ID = 2048

        fun startService(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
