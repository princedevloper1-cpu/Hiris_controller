package com.example.floatingeye.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.ui.FloatingEyePlusWidget
import com.example.floatingeye.ui.components.HirisOrbIllustration
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun EyeStudioScreen(
    config: FloatingEyeConfig,
    modifier: Modifier = Modifier
) {
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var isTrackingGaze by remember { mutableStateOf(false) }
    var bloomMultiplier by remember { mutableFloatStateOf(1.0f) }
    var selectedColorIndex by remember { mutableStateOf(0) }

    val neonColorPresets = listOf(
        Color(0xFF00F0FF), // Electric Cyan
        Color(0xFF38BDF8), // Sky Blue
        Color(0xFF10B981), // Emerald Green
        Color(0xFFA855F7), // Cyber Purple
        Color(0xFFF59E0B)  // Golden Solar
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                Text(
                    text = "Eye Studio",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Interactive 3D Gaze & Cybernetic Stage",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF7E90A6)
                )
            }
        }

        // Interactive Gaze Stage
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1525)),
                border = BorderStroke(1.dp, Color(0xFF1C2C46)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eye_studio_interactive_stage")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Gaze Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF14243B),
                            border = BorderStroke(0.8.dp, CyanNeonPrimary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isTrackingGaze) "TRACKING" else "IDLE",
                                color = CyanNeonPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Big Interactive Touch Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF090E18))
                            .border(BorderStroke(1.dp, Color(0xFF182438)), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        isTrackingGaze = true
                                        touchX = offset.x
                                        touchY = offset.y
                                        tryAwaitRelease()
                                        isTrackingGaze = false
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isTrackingGaze = true
                                        touchX = offset.x
                                        touchY = offset.y
                                    },
                                    onDragEnd = { isTrackingGaze = false },
                                    onDragCancel = { isTrackingGaze = false },
                                    onDrag = { change, _ ->
                                        touchX = change.position.x
                                        touchY = change.position.y
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        HirisOrbIllustration(
                            size = 140.dp,
                            animated = true
                        )

                        // Floating instruction HUD
                        Text(
                            text = "Touch and drag anywhere to direct gaze",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Coordinate readout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        CoordBox(label = "X", value = "${touchX.toInt()} px")
                        CoordBox(label = "Y", value = "${touchY.toInt()} px")
                        CoordBox(label = "GLOW", value = "${(bloomMultiplier * 100).toInt()}%")
                    }
                }
            }
        }

        // Lighting & Color Customizer
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1524)),
                border = BorderStroke(1.dp, Color(0xFF1B273F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Iris Neon Presets",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        neonColorPresets.forEachIndexed { index, color ->
                            val isSelected = selectedColorIndex == index
                            Surface(
                                onClick = { selectedColorIndex = index },
                                shape = CircleShape,
                                color = color,
                                border = BorderStroke(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent
                                ),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Atmospheric Glow Multiplier",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Slider(
                        value = bloomMultiplier,
                        onValueChange = { bloomMultiplier = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeonPrimary,
                            activeTrackColor = CyanNeonPrimary,
                            inactiveTrackColor = Color(0xFF1B2638)
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun CoordBox(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF121B2C),
        border = BorderStroke(0.8.dp, Color(0xFF1D2A42))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, color = CyanNeonPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
