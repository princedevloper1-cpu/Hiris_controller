package com.example.floatingeye.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FloatingEyeOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var positionManager: FloatingEyePositionManager
    private lateinit var overlayController: FloatingEyeOverlayController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        positionManager = FloatingEyePositionManager(this)
        overlayController = FloatingEyeOverlayController(this, positionManager)

        createNotificationChannel()
        _isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopOverlayAndSelf()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_VISIBILITY -> {
                val isVisible = overlayController.toggleVisibility()
                updateNotification(isVisible)
            }

            ACTION_UPDATE_SIZE -> {
                val sizeName = intent?.getStringExtra(EXTRA_SIZE_NAME)
                if (sizeName != null) {
                    try {
                        val size = OverlaySize.valueOf(sizeName)
                        overlayController.updateSize(size)
                    } catch (_: Exception) {}
                }
            }

            ACTION_UPDATE_BLOOM_STRENGTH -> {
                val strength = intent?.getFloatExtra(EXTRA_BLOOM_STRENGTH, Float.NaN) ?: Float.NaN
                if (strength.isFinite()) {
                    overlayController.updateBloomStrength(strength)
                }
            }

            ACTION_UPDATE_GLOW_INTENSITY -> {
                val intensity = intent?.getFloatExtra(EXTRA_GLOW_INTENSITY, Float.NaN) ?: Float.NaN
                if (intensity.isFinite()) {
                    overlayController.updateGlowIntensity(intensity)
                }
            }

            ACTION_START -> {
                startForegroundWithNotification(isEyeVisible = true)
                overlayController.showOverlay(
                    lifecycleOwner = this,
                    viewModelStoreOwner = this,
                    savedStateRegistryOwner = this
                )
                positionManager.saveOverlayEnabled(true)
                _isServiceRunning.value = true
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification(isEyeVisible: Boolean) {
        val notification = buildNotification(isEyeVisible)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(isEyeVisible: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(isEyeVisible))
    }

    private fun buildNotification(isEyeVisible: Boolean): Notification {
        // Tap notification to open main app
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Toggle visibility action ("Hide Eye" / "Show Eye")
        val toggleIntent = Intent(this, FloatingEyeOverlayService::class.java).apply {
            action = ACTION_TOGGLE_VISIBILITY
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleActionTitle = if (isEyeVisible) "Hide Eye" else "Show Eye"

        // Stop action
        val stopIntent = Intent(this, FloatingEyeOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hiris Controller Active")
            .setContentText(if (isEyeVisible) "Hovering over apps • Tap or drag to interact" else "Hiris Controller is currently hidden")
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, toggleActionTitle, togglePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hiris Controller",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls persistent floating cybernetic companion over applications"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopOverlayAndSelf() {
        overlayController.removeOverlay()
        positionManager.saveOverlayEnabled(false)
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        overlayController.removeOverlay()
        positionManager.saveOverlayEnabled(false)
        _isServiceRunning.value = false

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 4040
        private const val CHANNEL_ID = "floating_eye_overlay_channel"

        const val ACTION_START = "com.example.floatingeye.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.floatingeye.action.STOP_OVERLAY"
        const val ACTION_TOGGLE_VISIBILITY = "com.example.floatingeye.action.TOGGLE_VISIBILITY"
        const val ACTION_UPDATE_SIZE = "com.example.floatingeye.action.UPDATE_SIZE"
        const val ACTION_UPDATE_BLOOM_STRENGTH = "com.example.floatingeye.action.UPDATE_BLOOM_STRENGTH"
        const val ACTION_UPDATE_GLOW_INTENSITY = "com.example.floatingeye.action.UPDATE_GLOW_INTENSITY"
        const val EXTRA_SIZE_NAME = "extra_size_name"
        const val EXTRA_BLOOM_STRENGTH = "extra_bloom_strength"
        const val EXTRA_GLOW_INTENSITY = "extra_glow_intensity"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, FloatingEyeOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingEyeOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleVisibility(context: Context) {
            val intent = Intent(context, FloatingEyeOverlayService::class.java).apply {
                action = ACTION_TOGGLE_VISIBILITY
            }
            context.startService(intent)
        }

        fun updateSize(context: Context, size: OverlaySize) {
            val intent = Intent(context, FloatingEyeOverlayService::class.java).apply {
                action = ACTION_UPDATE_SIZE
                putExtra(EXTRA_SIZE_NAME, size.name)
            }
            context.startService(intent)
        }

        fun updateBloomStrength(context: Context, strength: Float) {
            if (!_isServiceRunning.value) return
            val intent = Intent(context, FloatingEyeOverlayService::class.java).apply {
                action = ACTION_UPDATE_BLOOM_STRENGTH
                putExtra(EXTRA_BLOOM_STRENGTH, strength.coerceIn(0f, 1f))
            }
            context.startService(intent)
        }

        fun updateGlowIntensity(context: Context, intensity: Float) {
            if (!_isServiceRunning.value) return
            val intent = Intent(context, FloatingEyeOverlayService::class.java).apply {
                action = ACTION_UPDATE_GLOW_INTENSITY
                putExtra(EXTRA_GLOW_INTENSITY, intensity.coerceIn(0.5f, 2.5f))
            }
            context.startService(intent)
        }
    }
}
