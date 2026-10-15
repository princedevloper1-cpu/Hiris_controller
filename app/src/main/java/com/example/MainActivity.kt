package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.ui.HirisMainScreen
import com.example.ui.theme.FloatingEyeTheme
import com.example.localization.AppLanguage

class MainActivity : ComponentActivity() {
    private var targetExtensionId by mutableStateOf<String?>(null)
    private var firewallVpnRequestId by mutableStateOf(0)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LegacyCleanWorkCleanup.run(this)
        enableEdgeToEdge()

        val positionManager = FloatingEyePositionManager(this)
        val config = FloatingEyeConfig.loadFromAssets(this)
        targetExtensionId = intent?.getStringExtra("open_extension_id")
        if (intent?.getBooleanExtra("request_firewall_vpn", false) == true) {
            firewallVpnRequestId++
        }

        setContent {
            FloatingEyeTheme {
                HirisMainScreen(
                    positionManager = positionManager,
                    config = config,
                    initialExtensionId = targetExtensionId,
                    firewallVpnRequestId = firewallVpnRequestId
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("open_extension_id")?.let { extId ->
            targetExtensionId = extId
        }
        if (intent.getBooleanExtra("request_firewall_vpn", false)) {
            firewallVpnRequestId++
        }
    }
}

