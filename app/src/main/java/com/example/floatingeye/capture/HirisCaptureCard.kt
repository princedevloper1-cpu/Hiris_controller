package com.example.floatingeye.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun HirisCaptureCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(HirisCaptureService.MODE_SCREENSHOT) }
    var areaMode by remember { mutableStateOf(HirisCaptureService.AREA_RECTANGLE) }
    var countdown by remember { mutableIntStateOf(0) }
    var requestCapture by remember { mutableStateOf(false) }
    LaunchedEffect(countdown, requestCapture) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        } else if (requestCapture) {
            requestCapture = false
            HirisCaptureConsentActivity.launch(context, mode, areaMode)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Hiris Capture", style = MaterialTheme.typography.titleLarge)
            Text("Select what to capture. Android will ask for screen-capture permission each time.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mode == HirisCaptureService.MODE_SCREENSHOT) Button(onClick = {}) { Text("Screenshot") }
                else OutlinedButton(onClick = { mode = HirisCaptureService.MODE_SCREENSHOT }) { Text("Screenshot") }
                if (mode == HirisCaptureService.MODE_RECORD) Button(onClick = {}) { Text("Screen record") }
                else OutlinedButton(onClick = { mode = HirisCaptureService.MODE_RECORD }) { Text("Screen record") }
            }
            if (mode == HirisCaptureService.MODE_SCREENSHOT) {
                Text("Capture area", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = areaMode == HirisCaptureService.AREA_FULL,
                        onClick = { areaMode = HirisCaptureService.AREA_FULL },
                        label = { Text("Full screen") }
                    )
                    FilterChip(
                        selected = areaMode == HirisCaptureService.AREA_RECTANGLE,
                        onClick = { areaMode = HirisCaptureService.AREA_RECTANGLE },
                        label = { Text("Select rectangle") }
                    )
                }
                Text(
                    if (areaMode == HirisCaptureService.AREA_RECTANGLE)
                        "After capture, tap the Hiris notification to draw and save the area you want."
                    else "The complete screen will be saved as a screenshot."
                )
            } else {
                Text("Screen recording captures the full screen.")
            }
            if (countdown > 0) Text("Starting in $countdown…", style = MaterialTheme.typography.headlineMedium)
            Button(
                enabled = countdown == 0,
                onClick = {
                    if (mode == HirisCaptureService.MODE_RECORD) {
                        countdown = 3
                        requestCapture = true
                    } else {
                        requestCapture = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (mode == HirisCaptureService.MODE_RECORD) "Start recording" else "Take screenshot") }
            if (mode == HirisCaptureService.MODE_RECORD) {
                Text("Pause, resume and stop from the persistent Hiris Capture notification.")
            }
        }
    }
}
