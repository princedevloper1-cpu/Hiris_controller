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
    private var selectionEditorOwnsOverlayRestore = false
    private var waitingForRecordRegion = false
    private var cropRenderer: RegionCropRenderer? = null
    private var isStopping = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val regionSelectionTimeout = Runnable {
        if (waitingForRecordRegion) stopCapture()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopCapture()
            ACTION_PAUSE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) recorder?.pause()
            ACTION_RESUME -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) recorder?.resume()
            ACTION_START -> beginProjection(intent)
            ACTION_START_RECORDING_AREA -> try {
                startRecording(
                    checkNotNull(projection),
                    RecordingRegion(
                        left = intent.getIntExtra(EXTRA_CROP_LEFT, 0),
                        top = intent.getIntExtra(EXTRA_CROP_TOP, 0),
                        right = intent.getIntExtra(EXTRA_CROP_RIGHT, 0),
                        bottom = intent.getIntExtra(EXTRA_CROP_BOTTOM, 0),
                        sourceWidth = intent.getIntExtra(EXTRA_SOURCE_WIDTH, 0),
                        sourceHeight = intent.getIntExtra(EXTRA_SOURCE_HEIGHT, 0)
                    )
                )
            } catch (error: Exception) {
                android.widget.Toast.makeText(this, "Could not record selected area: ${error.localizedMessage ?: "unknown error"}", android.widget.Toast.LENGTH_LONG).show()
                stopCapture()
            }
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
        if (resultData == null) {
            finishConsentFlow()
            stopCapture()
            return
        }

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SCREENSHOT
        mainHandler.postDelayed({
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = manager.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                finishConsentFlow()
                stopCapture()
                return@postDelayed
            }
            projection = mediaProjection
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { if (!isStopping) stopCapture() }
            }, mainHandler)
            try {
                val areaMode = intent.getStringExtra(EXTRA_AREA_MODE) ?: AREA_FULL
                if (mode == MODE_RECORD && areaMode == AREA_RECTANGLE) {
                    waitingForRecordRegion = true
                    takeScreenshot(mediaProjection, areaMode, forRecording = true)
                } else if (mode == MODE_RECORD) {
                    startRecording(mediaProjection)
                } else {
                    takeScreenshot(mediaProjection, areaMode)
                }
            } catch (error: Exception) {
                android.widget.Toast.makeText(this, "Hiris Capture could not start: ${error.localizedMessage ?: "unknown error"}", android.widget.Toast.LENGTH_LONG).show()
                finishConsentFlow()
                stopCapture()
            }
        }, 500L)
    }

    private fun screenSize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun takeScreenshot(mediaProjection: MediaProjection, areaMode: String, forRecording: Boolean = false) {
        val (width, height) = screenSize()
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader = imageReader
        display = mediaProjection.createVirtualDisplay(
            "Hiris Capture Screenshot", width, height, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, mainHandler
        )
        imageReader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            // We only need one frozen frame for the selection UI. Detach before
            // opening it so queued frames cannot launch multiple selector activities.
            source.setOnImageAvailableListener(null, null)
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
                if (areaMode == AREA_RECTANGLE) {
                    val pendingImage = File(cacheDir, "hiris_selection_${timestamp()}.png")
                    FileOutputStream(pendingImage).use { cropped.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    if (forRecording) {
                        if (!openSelectionEditor(pendingImage, forRecording = true)) {
                            waitingForRecordRegion = false
                        }
                    } else if (!openSelectionEditor(pendingImage)) {
                        saveScreenshot(cropped)
                    }
                } else {
                    saveScreenshot(cropped)
                }
                if (cropped !== bitmap) cropped.recycle()
                bitmap.recycle()
            } finally {
                image.close()
                if (forRecording && waitingForRecordRegion) {
                    display?.setSurface(null)
                    reader?.close()
                    reader = null
                    mainHandler.postDelayed(regionSelectionTimeout, REGION_SELECTION_TIMEOUT_MS)
                    updateRecordingNotification("Draw the area to record in Hiris Capture")
                } else {
                    stopCapture()
                }
            }
        }, mainHandler)
        mainHandler.postDelayed({ imageReader.acquireLatestImage()?.close() }, 1200L)
    }

    private fun saveScreenshot(bitmap: android.graphics.Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Hiris_${timestamp()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hiris Capture")
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun openSelectionEditor(image: File, forRecording: Boolean = false): Boolean = try {
            val intent = Intent(this, HirisCaptureSelectionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                putExtra(EXTRA_IMAGE_PATH, image.absolutePath)
                putExtra(EXTRA_RESTORE_OVERLAY, overlayWasRunning && !forRecording)
                putExtra(EXTRA_SELECTION_FOR_RECORDING, forRecording)
            }
            startActivity(intent)
            selectionEditorOwnsOverlayRestore = overlayWasRunning && !forRecording
            sendBroadcast(Intent(HirisCaptureConsentActivity.ACTION_SELECTION_EDITOR_OPENED).setPackage(packageName))
            true
        } catch (_: Exception) {
            image.delete()
            finishConsentFlow()
            android.widget.Toast.makeText(
                this,
                if (forRecording) "Area selection could not open. Recording cancelled."
                else "Area selection could not open. Full screenshot saved.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            false
        }

    private fun finishConsentFlow() {
        sendBroadcast(Intent(HirisCaptureConsentActivity.ACTION_FINISH_CAPTURE_FLOW).setPackage(packageName))
    }

    private fun startRecording(mediaProjection: MediaProjection, region: RecordingRegion? = null) {
        waitingForRecordRegion = false
        mainHandler.removeCallbacks(regionSelectionTimeout)
        val (width, height) = screenSize()
        val scaleX = if (region != null && region.sourceWidth > 0) width.toFloat() / region.sourceWidth else 1f
        val scaleY = if (region != null && region.sourceHeight > 0) height.toFloat() / region.sourceHeight else 1f
        val cropLeft = region?.let { (it.left * scaleX).toInt().coerceIn(0, width - 2) } ?: 0
        val cropTop = region?.let { (it.top * scaleY).toInt().coerceIn(0, height - 2) } ?: 0
        val cropRight = region?.let { (it.right * scaleX).toInt().coerceIn(cropLeft + 2, width) } ?: width
        val cropBottom = region?.let { (it.bottom * scaleY).toInt().coerceIn(cropTop + 2, height) } ?: height
        val evenWidth = (cropRight - cropLeft) - ((cropRight - cropLeft) % 2)
        val evenHeight = (cropBottom - cropTop) - ((cropBottom - cropTop) % 2)
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
        if (region == null) {
            display = mediaProjection.createVirtualDisplay(
                "Hiris Capture Recording", evenWidth, evenHeight, resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.surface, null, mainHandler
            )
            mediaRecorder.start()
        } else {
            cropRenderer = RegionCropRenderer(
                width, height, cropLeft, cropTop, evenWidth, evenHeight, mediaRecorder.surface
            )
            mediaRecorder.start()
            checkNotNull(display) { "Screen projection is no longer available" }.setSurface(cropRenderer?.inputSurface)
        }
        updateRecordingNotification("Recording screen • use notification controls")
    }

    private fun stopCapture() {
        if (isStopping) return
        isStopping = true
        mainHandler.removeCallbacksAndMessages(null)
        display?.setSurface(null)
        cropRenderer?.release(); cropRenderer = null
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
        if (overlayWasRunning && !selectionEditorOwnsOverlayRestore && !FloatingEyeOverlayService.isServiceRunning.value) {
            FloatingEyeOverlayService.start(this)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.hiris.capture.START"
        const val ACTION_PAUSE = "com.example.hiris.capture.PAUSE"
        const val ACTION_RESUME = "com.example.hiris.capture.RESUME"
        const val ACTION_STOP = "com.example.hiris.capture.STOP"
        const val ACTION_START_RECORDING_AREA = "com.example.hiris.capture.START_RECORDING_AREA"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_MODE = "capture_mode"
        const val EXTRA_AREA_MODE = "capture_area_mode"
        const val EXTRA_IMAGE_PATH = "capture_image_path"
        const val EXTRA_RESTORE_OVERLAY = "restore_overlay_after_selection"
        const val EXTRA_SELECTION_FOR_RECORDING = "selection_for_recording"
        const val EXTRA_CROP_LEFT = "crop_left"
        const val EXTRA_CROP_TOP = "crop_top"
        const val EXTRA_CROP_RIGHT = "crop_right"
        const val EXTRA_CROP_BOTTOM = "crop_bottom"
        const val EXTRA_SOURCE_WIDTH = "source_width"
        const val EXTRA_SOURCE_HEIGHT = "source_height"
        const val MODE_SCREENSHOT = "screenshot"
        const val MODE_RECORD = "record"
        const val AREA_FULL = "full"
        const val AREA_RECTANGLE = "rectangle"
        private const val CHANNEL_ID = "hiris_capture_channel"
        private const val NOTIFICATION_ID = 7091
        private const val REGION_SELECTION_TIMEOUT_MS = 120_000L

        fun start(context: Context, resultCode: Int, resultData: Intent, mode: String, areaMode: String) {
            val intent = Intent(context, HirisCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_AREA_MODE, areaMode)
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        fun startRecordingArea(context: Context, left: Int, top: Int, right: Int, bottom: Int, sourceWidth: Int, sourceHeight: Int) {
            val intent = Intent(context, HirisCaptureService::class.java).apply {
                action = ACTION_START_RECORDING_AREA
                putExtra(EXTRA_CROP_LEFT, left)
                putExtra(EXTRA_CROP_TOP, top)
                putExtra(EXTRA_CROP_RIGHT, right)
                putExtra(EXTRA_CROP_BOTTOM, bottom)
                putExtra(EXTRA_SOURCE_WIDTH, sourceWidth)
                putExtra(EXTRA_SOURCE_HEIGHT, sourceHeight)
            }
            context.startService(intent)
        }
    }

    private data class RecordingRegion(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val sourceWidth: Int,
        val sourceHeight: Int
    )
}
