package com.example.floatingeye.capture

import android.app.Activity
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** Small dedicated activity keeps projection consent available from both app and overlay UI. */
class HirisCaptureConsentActivity : ComponentActivity() {
    private val finishFlowReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == ACTION_FINISH_CAPTURE_FLOW) moveTaskToBack(true)
            finish()
        }
    }

    private val projectionConsent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val mode = intent.getStringExtra(HirisCaptureService.EXTRA_MODE)
                ?: HirisCaptureService.MODE_SCREENSHOT
            val areaMode = intent.getStringExtra(HirisCaptureService.EXTRA_AREA_MODE)
                ?: HirisCaptureService.AREA_FULL
            HirisCaptureService.start(
                this,
                result.resultCode,
                data,
                mode,
                areaMode
            )
            if (areaMode == HirisCaptureService.AREA_RECTANGLE) {
                // Keep this transparent activity visible behind capture. That lets the
                // service open the area selector immediately without a notification.
            } else {
                moveTaskToBack(true)
                finish()
            }
        } else {
            android.widget.Toast.makeText(this, "Screen capture permission was not granted", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            finishFlowReceiver,
            IntentFilter().apply {
                addAction(ACTION_FINISH_CAPTURE_FLOW)
                addAction(ACTION_SELECTION_EDITOR_OPENED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionConsent.launch(manager.createScreenCaptureIntent())
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(finishFlowReceiver) }
        super.onDestroy()
    }

    companion object {
        const val ACTION_FINISH_CAPTURE_FLOW = "com.example.hiris.capture.FINISH_FLOW"
        const val ACTION_SELECTION_EDITOR_OPENED = "com.example.hiris.capture.SELECTION_OPENED"

        fun launch(context: Context, mode: String, areaMode: String) {
            context.startActivity(Intent(context, HirisCaptureConsentActivity::class.java).apply {
                putExtra(HirisCaptureService.EXTRA_MODE, mode)
                putExtra(HirisCaptureService.EXTRA_AREA_MODE, areaMode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            })
        }
    }
}
