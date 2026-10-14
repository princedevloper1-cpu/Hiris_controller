package com.example.floatingeye.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.floatingeye.overlay.FloatingEyeOverlayService
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Owns one user-approved MediaProjection session for Hiris Capture. */
class HirisCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var reader: ImageReader? = null
    private var tempVideo: File? = null
    private var overlayWasRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopCapture()
            ACTION_PAUSE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) recorder?.pause()
            ACTION_RESUME -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) recorder?.resume()
            ACTION_START -> beginProjection(intent)
        }
        return START_NOT_STICKY
    }

    private fun beginProjection(intent: Intent) {
        if (projection != null) return
        overlayWasRunning = FloatingEyeOverlayService.isServiceRunning.value
        if (overlayWasRunning) FloatingEyeOverlayService.stop(this)
        startCaptureForeground()

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        @Suppress("DEPRECATION")
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else intent.getParcelableExtra(EXTRA_RESULT_DATA)
        if (resultData == null) { stopCapture(); return }

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SCREENSHOT
        mainHandler.postDelayed({
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = manager.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) { stopCapture(); return@postDelayed }
            projection = mediaProjection
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopCapture() }
            }, mainHandler)
            if (mode == MODE_RECORD) startRecording(mediaProjection) else takeScreenshot(mediaProjection)
        }, 500L)
    }

    private fun screenSize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun takeScreenshot(mediaProjection: MediaProjection) {
        val (width, height) = screenSize()
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader = imageReader
        display = mediaProjection.createVirtualDisplay(
            "Hiris Capture Screenshot", width, height, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, mainHandler
        )
        imageReader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val bitmap = android.graphics.Bitmap.createBitmap(
                    rowStride / pixelStride, height, android.graphics.Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                val cropped = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Hiris_${timestamp()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hiris Capture")
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { cropped.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                }
                if (cropped !== bitmap) cropped.recycle()
                bitmap.recycle()
            } finally { image.close(); stopCapture() }
        }, mainHandler)
        mainHandler.postDelayed({ imageReader.acquireLatestImage()?.close() }, 1200L)
    }

    private fun startRecording(mediaProjection: MediaProjection) {
        val (width, height) = screenSize()
        val evenWidth = width - (width % 2)
        val evenHeight = height - (height % 2)
        val output = File(cacheDir, "hiris_${timestamp()}.mp4")
        tempVideo = output
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        recorder = mediaRecorder
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setVideoSize(evenWidth, evenHeight)
        mediaRecorder.setVideoFrameRate(30)
        mediaRecorder.setVideoEncodingBitRate(6_000_000)
        mediaRecorder.setOutputFile(output.absolutePath)
        mediaRecorder.prepare()
        display = mediaProjection.createVirtualDisplay(
            "Hiris Capture Recording", evenWidth, evenHeight, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.surface, null, mainHandler
        )
        mediaRecorder.start()
        updateRecordingNotification("Recording screen • use notification controls")
    }

    private fun stopCapture() {
        mainHandler.removeCallbacksAndMessages(null)
        try { recorder?.stop() } catch (_: RuntimeException) { tempVideo?.delete() }
        recorder?.release(); recorder = null
        display?.release(); display = null
        reader?.close(); reader = null
        val activeProjection = projection
        projection = null
        activeProjection?.let { p -> try { p.stop() } catch (_: Exception) {} }
        tempVideo?.let { file -> if (file.exists() && file.length() > 0) publishVideo(file) }
        tempVideo = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishVideo(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Hiris_${file.nameWithoutExtension}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Hiris Capture")
        }
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
        }
        file.delete()
    }

    private fun startCaptureForeground() {
        createChannel()
        val notification = buildNotification("Preparing screen capture")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateRecordingNotification(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        fun action(label: String, action: String, requestCode: Int): NotificationCompat.Action {
            val pending = android.app.PendingIntent.getService(
                this, requestCode, Intent(this, HirisCaptureService::class.java).setAction(action),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action.Builder(0, label, pending).build()
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Hiris Capture").setContentText(text)
            .setOngoing(true).setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(action("Pause", ACTION_PAUSE, 1))
            .addAction(action("Resume", ACTION_RESUME, 2))
            .addAction(action("Stop", ACTION_STOP, 3)).build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Hiris screen capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    override fun onDestroy() {
        if (projection != null || recorder != null) stopCapture()
        if (overlayWasRunning && !FloatingEyeOverlayService.isServiceRunning.value) FloatingEyeOverlayService.start(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.hiris.capture.START"
        const val ACTION_PAUSE = "com.example.hiris.capture.PAUSE"
        const val ACTION_RESUME = "com.example.hiris.capture.RESUME"
        const val ACTION_STOP = "com.example.hiris.capture.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_MODE = "capture_mode"
        const val MODE_SCREENSHOT = "screenshot"
        const val MODE_RECORD = "record"
        private const val CHANNEL_ID = "hiris_capture_channel"
        private const val NOTIFICATION_ID = 7091

        fun start(context: Context, resultCode: Int, resultData: Intent, mode: String) {
            val intent = Intent(context, HirisCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_MODE, mode)
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
