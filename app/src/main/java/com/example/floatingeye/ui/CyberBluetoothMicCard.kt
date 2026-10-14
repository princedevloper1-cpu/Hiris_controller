package com.example.floatingeye.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.audio.BluetoothMicController
import com.example.floatingeye.audio.MicOutputRoute
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CyberBluetoothMicCard(
    micController: BluetoothMicController,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val context = LocalContext.current
    val isPlaying by micController.isMicPlaying.collectAsStateWithLifecycle()
    val audioLevel by micController.audioLevel.collectAsStateWithLifecycle()
    val isBtConnected by micController.isBluetoothConnected.collectAsStateWithLifecycle()
    val isAuxConnected by micController.isAuxConnected.collectAsStateWithLifecycle()
    val connectedDevice by micController.connectedDeviceName.collectAsStateWithLifecycle()
    val inputDevice by micController.inputDeviceName.collectAsStateWithLifecycle()
    val outputRoutes by micController.availableOutputRoutes.collectAsStateWithLifecycle()
    val selectedOutputRoute by micController.selectedOutputRoute.collectAsStateWithLifecycle()
    val gain by micController.micGain.collectAsStateWithLifecycle()
    val isMuted by micController.isMuted.collectAsStateWithLifecycle()
    val errorMsg by micController.errorMessage.collectAsStateWithLifecycle()

    var isExpanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        micController.checkBluetoothStatus()
        if (micController.hasRecordPermission() && !isPlaying) {
            micController.startStreaming()
        }
    }

    fun toggleMic() {
        if (isPlaying) {
            micController.stopStreaming()
            return
        }
        val permissions = buildList {
            if (!micController.hasRecordPermission()) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                selectedOutputRoute == MicOutputRoute.BLUETOOTH &&
                !micController.hasBluetoothConnectPermission()
            ) add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
        else micController.startStreaming()
    }

    LaunchedEffect(Unit) {
        micController.checkBluetoothStatus()
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isPlaying) Color(0xFF38E07B).copy(alpha = 0.7f) else CyanNeonPrimary.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cyber_bluetooth_mic_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Device status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlaying) Color(0xFF003D20) else SurfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                contentDescription = "Mic",
                                tint = if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Mic & Audio Routing",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isBtConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = if (isBtConnected) CyanNeonPrimary else TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = connectedDevice,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBtConnected) CyanNeonPrimary else TextSecondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Live Active indicator pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPlaying) Color(0xFF003D20) else SurfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPlaying) Color(0xFF38E07B) else SurfaceContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isPlaying) Color(0xFF38E07B) else Color(0xFF6B7280),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (isPlaying) "LIVE" else "OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPlaying) Color(0xFF38E07B) else TextTertiary
                            )
                        }
                    }

                    // Space-reduction Chip
                    FilterChip(
                        selected = isExpanded,
                        onClick = { isExpanded = !isExpanded },
                        label = {
                            Text(
                                text = if (isExpanded) "Collapse" else "Expand",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeonPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanNeonPrimary,
                            selectedLeadingIconColor = CyanNeonPrimary,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary,
                            iconColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isExpanded) CyanNeonPrimary else Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("chip_mic_collapse")
                    )
                }
            }

            // Compact Mode row when collapsed to save space
            if (!isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaying) "Streaming to $connectedDevice" else "Choose Bluetooth or wired AUX output",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPlaying) Color(0xFF38E07B) else TextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    )

                    Button(
                        onClick = {
                            toggleMic()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) Color(0xFFEF4444) else CyanNeonPrimary,
                            contentColor = if (isPlaying) Color.White else CyanOnPrimary
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("btn_compact_mic_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "STOP" else "PLAY", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Expanded Mode details
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Error alert if present
                    errorMsg?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3B1014),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30343B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("AUDIO ROUTING", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("INPUT", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Text(inputDevice, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("OUTPUT", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Text(connectedDevice, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                outputRoutes.forEach { route ->
                                    FilterChip(
                                        selected = selectedOutputRoute == route,
                                        onClick = { micController.setOutputRoute(route) },
                                        label = { Text(route.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyanNeonPrimary.copy(alpha = 0.2f),
                                            selectedLabelColor = CyanNeonPrimary
                                        )
                                    )
                                }
                            }
                            Text(
                                text = when (selectedOutputRoute) {
                                    MicOutputRoute.AUX -> if (isAuxConnected) {
                                        "Wired AUX selected for lower monitoring delay."
                                    } else {
                                        "AUX selected. Plug in wired headphones or a headset to use it."
                                    }
                                    MicOutputRoute.BLUETOOTH -> if (isBtConnected) {
                                        "Bluetooth selected. Delay depends on the headset."
                                    } else {
                                        "Bluetooth selected. Connect a Bluetooth headset or speaker first."
                                    }
                                    MicOutputRoute.AUTO -> "Auto uses Android's current audio route."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Real-Time Audio Equalizer / Visualizer Canvas
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LiveAudioVisualizerBars(
                                audioLevel = audioLevel,
                                isPlaying = isPlaying && !isMuted,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (!isPlaying) {
                                Text(
                                    text = "Microphone inactive • Tap START below to begin speaking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    // Controls: Main Big Play/Stop Button & Mute Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                toggleMic()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) Color(0xFFEF4444) else CyanNeonPrimary,
                                contentColor = if (isPlaying) Color.White else CyanOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_toggle_mic_stream")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "STOP BROADCAST" else "START BROADCAST",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // Mute / Unmute Button
                        IconButton(
                            onClick = { micController.toggleMute() },
                            enabled = isPlaying,
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    if (isMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else SurfaceContainerHigh,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isMuted) Color(0xFFEF4444) else CyanNeonPrimary.copy(alpha = 0.4f),
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = "Mute",
                                tint = if (isMuted) Color(0xFFEF4444) else CyanNeonPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Gain / Sensitivity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VolumeUp,
                                    contentDescription = null,
                                    tint = CyanNeonPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Mic Gain / Sensitivity",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${String.format("%.1f", gain)}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeonPrimary
                            )
                        }

                        Slider(
                            value = gain,
                            onValueChange = { micController.setGain(it) },
                            valueRange = 0.5f..3.5f,
                            steps = 6,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeonPrimary,
                                activeTrackColor = CyanNeonPrimary,
                                inactiveTrackColor = SurfaceContainerHigh
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Futuristic Hint pill for Floating Plus Button integration
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "When the Floating (+) widget is active on screen, tap it to reveal the floating mic icon to play/stop anytime!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 24 animated bouncing cybernetic VU bars responding dynamically to live microphone RMS volume.
 */
@Composable
private fun LiveAudioVisualizerBars(
    audioLevel: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = 3.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - 24.dp.toPx() - totalSpacing) / barCount).coerceAtLeast(3f)
        val startX = 12.dp.toPx()
        val centerY = size.height / 2f
        val maxHalfHeight = (size.height / 2f) - 6.dp.toPx()

        for (i in 0 until barCount) {
            val normIndex = (i - barCount / 2f) / (barCount / 2f)
            val bellFactor = kotlin.math.cos(normIndex * 1.3f).coerceIn(0.2f, 1f).toFloat()

            val barHeight = if (isPlaying) {
                val noise = kotlin.math.sin(idlePhase * 3f + i * 0.7f).toFloat() * 0.15f
                val effectiveLevel = (audioLevel * bellFactor * 1.4f + noise).coerceIn(0.08f, 1f)
                maxHalfHeight * effectiveLevel
            } else {
                3.dp.toPx()
            }

            val x = startX + i * (barWidth + spacing)
            val top = centerY - barHeight
            val bottom = centerY + barHeight

            val barColor = if (isPlaying) {
                if (audioLevel > 0.6f && (i in 10..18)) {
                    Color(0xFF38E07B) // Peak emerald green
                } else {
                    CyanNeonPrimary
                }
            } else {
                CyanNeonPrimary.copy(alpha = 0.25f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, (bottom - top).coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
