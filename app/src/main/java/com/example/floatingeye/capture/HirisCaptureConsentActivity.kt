package com.example.floatingeye.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Small dedicated activity keeps projection consent available from both app and overlay UI. */
class HirisCaptureConsentActivity : ComponentActivity() {
    private val projectionConsent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            HirisCaptureService.start(
                this,
                result.resultCode,
                data,
                intent.getStringExtra(HirisCaptureService.EXTRA_MODE)
                    ?: HirisCaptureService.MODE_SCREENSHOT
            )
        }
        moveTaskToBack(true)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionConsent.launch(manager.createScreenCaptureIntent())
    }

    companion object {
        fun launch(context: Context, mode: String) {
            context.startActivity(Intent(context, HirisCaptureConsentActivity::class.java).apply {
                putExtra(HirisCaptureService.EXTRA_MODE, mode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
