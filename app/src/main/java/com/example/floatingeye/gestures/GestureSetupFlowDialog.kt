package com.example.floatingeye.gestures

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.shortcuts.DrawableAppIcon
import com.example.floatingeye.shortcuts.InstalledAppInfo
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class SetupStep {
    SELECT_APP,
    DRAW_GESTURE
}

@Composable
fun GestureSetupFlowDialog(
    installedAppsRepository: InstalledAppsRepository,
    gestureRepository: GestureRepository,
    config: FloatingEyeConfig,
    editingBinding: GestureAppBinding? = null,
    onDismiss: () -> Unit,
    onCompleted: (GestureAppBinding) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember {
        mutableStateOf(if (editingBinding != null) SetupStep.DRAW_GESTURE else SetupStep.SELECT_APP)
    }

    var selectedApp by remember {
        mutableStateOf<InstalledAppInfo?>(
            if (editingBinding != null) {
                InstalledAppInfo(
                    packageName = editingBinding.packageName,
                    displayName = editingBinding.appName,
                    icon = installedAppsRepository.getAppIcon(editingBinding.packageName)
                )
            } else null
        )
    }

    val captureController = remember { GestureCaptureController() }
    var similarityWarning by remember { mutableStateOf<Pair<GestureAppBinding, Float>?>(null) }
    var rawDrawnPoints by remember { mutableStateOf<List<GesturePoint>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentStep != SetupStep.SELECT_APP && editingBinding == null) {
                            IconButton(
                                onClick = {
                                    currentStep = SetupStep.SELECT_APP
                                    captureController.clear()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextSecondary
                                )
                            }
                        }

                        Column {
                            Text(
                                text = when (currentStep) {
                                    SetupStep.SELECT_APP -> "1. Select an Application"
                                    SetupStep.DRAW_GESTURE -> "2. Draw Gesture Shape"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            selectedApp?.let {
                                Text(
                                    text = "Bound to: ${it.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanNeonPrimary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(targetState = currentStep, label = "step_content") { step ->
                    when (step) {
                        SetupStep.SELECT_APP -> {
                            StepSelectApp(
                                installedAppsRepository = installedAppsRepository,
                                onAppSelected = { app ->
                                    selectedApp = app
                                    captureController.clear()
                                    currentStep = SetupStep.DRAW_GESTURE
                                }
                            )
                        }

                        SetupStep.DRAW_GESTURE -> {
                            StepDrawAndSave(
                                appInfo = selectedApp,
                                captureController = captureController,
                                onClear = {
                                    captureController.clear()
                                    rawDrawnPoints = emptyList()
                                },
                                onSave = { points ->
                                    val currentApp = selectedApp
                                    if (points.size >= 4 && currentApp != null) {
                                        rawDrawnPoints = points
                                        val norm = GestureRecognizer.normalize(points)
                                        if (norm.isEmpty()) {
                                            Toast.makeText(context, "Gesture too short, please draw again", Toast.LENGTH_SHORT).show()
                                            return@StepDrawAndSave
                                        }

                                        val similar = gestureRepository.findSimilarGesture(
                                            norm,
                                            excludeId = editingBinding?.id
                                        )

                                        if (similar != null) {
                                            similarityWarning = similar
                                        } else {
                                            val finalBinding = GestureAppBinding(
                                                id = editingBinding?.id ?: java.util.UUID.randomUUID().toString(),
                                                packageName = currentApp.packageName,
                                                appName = currentApp.displayName,
                                                normalizedPoints = norm,
                                                rawPoints = points,
                                                isEnabled = editingBinding?.isEnabled ?: true
                                            )
                                            gestureRepository.saveGesture(finalBinding)
                                            Toast.makeText(
                                                context,
                                                "Gesture for \"${currentApp.displayName}\" saved and activated!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onCompleted(finalBinding)
                                        }
                                    } else {
                                        Toast.makeText(context, "Draw a symbol on the canvas before saving", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Similarity warning dialog
            similarityWarning?.let { (existing, score) ->
                AlertDialog(
                    onDismissRequest = { similarityWarning = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text("Similar Gesture Detected", style = MaterialTheme.typography.titleMedium)
                    },
                    text = {
                        Text(
                            "This symbol closely matches your gesture for \"${existing.appName}\" (${(score * 100).toInt()}% match). Would you like to save anyway or redraw?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val currentApp = selectedApp
                                if (currentApp != null && rawDrawnPoints.isNotEmpty()) {
                                    val norm = GestureRecognizer.normalize(rawDrawnPoints)
                                    val finalBinding = GestureAppBinding(
                                        id = editingBinding?.id ?: java.util.UUID.randomUUID().toString(),
                                        packageName = currentApp.packageName,
                                        appName = currentApp.displayName,
                                        normalizedPoints = norm,
                                        rawPoints = rawDrawnPoints,
                                        isEnabled = editingBinding?.isEnabled ?: true
                                    )
                                    gestureRepository.saveGesture(finalBinding)
                                    Toast.makeText(
                                        context,
                                        "Gesture for \"${currentApp.displayName}\" saved and activated!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    similarityWarning = null
                                    onCompleted(finalBinding)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeonPrimary)
                        ) {
                            Text("Save Anyway", color = CyanOnPrimary)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                similarityWarning = null
                                captureController.clear()
                            }
                        ) {
                            Text("Redraw", color = TextPrimary)
                        }
                    },
                    containerColor = SurfaceContainerHigh
                )
            }
        }
    }
}

@Composable
private fun StepSelectApp(
    installedAppsRepository: InstalledAppsRepository,
    onAppSelected: (InstalledAppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = installedAppsRepository.getInstalledLaunchableApps()
        isLoading = false
    }

    val filtered = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else {
            apps.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search applications...", color = TextTertiary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeonPrimary,
                unfocusedBorderColor = SurfaceContainer,
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanNeonPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.packageName }) { appInfo ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(appInfo) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    DrawableAppIcon(
                                        drawable = appInfo.icon,
                                        contentDescription = appInfo.displayName,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appInfo.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = appInfo.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2D Pure Compose Eye Tracker widget that safely follows finger movements.
 * Completely immune to native Filament/OpenGL crashes in Dialogs.
 */
@Composable
private fun SafeCyberneticEyeTracker(
    touchX: Float,
    touchY: Float,
    modifier: Modifier = Modifier
) {
    val animDeflectionX by animateFloatAsState(
        targetValue = ((touchX - 250f) / 300f).coerceIn(-1f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "eye_deflection_x"
    )
    val animDeflectionY by animateFloatAsState(
        targetValue = ((touchY - 250f) / 300f).coerceIn(-1f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "eye_deflection_y"
    )

    Surface(
        shape = CircleShape,
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(2.dp, CyanNeonPrimary.copy(alpha = 0.8f)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 4.dp.toPx()

            // Outer Iris ring
            drawCircle(
                color = Color(0xFF00384D),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = CyanNeonPrimary.copy(alpha = 0.4f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Deflected Pupil center
            val maxOffset = outerRadius * 0.45f
            val pupilCenter = Offset(
                x = center.x + animDeflectionX * maxOffset,
                y = center.y + animDeflectionY * maxOffset
            )

            // Pupil glow and core
            drawCircle(
                color = CyanNeonPrimary,
                radius = outerRadius * 0.40f,
                center = pupilCenter
            )
            drawCircle(
                color = Color.Black,
                radius = outerRadius * 0.22f,
                center = pupilCenter
            )
            // Specular highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = outerRadius * 0.08f,
                center = Offset(pupilCenter.x - 3.dp.toPx(), pupilCenter.y - 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun StepDrawAndSave(
    appInfo: InstalledAppInfo?,
    captureController: GestureCaptureController,
    onClear: () -> Unit,
    onSave: (List<GesturePoint>) -> Unit
) {
    val points by captureController.points.collectAsStateWithLifecycle()
    var lastPointerX by remember { mutableFloatStateOf(250f) }
    var lastPointerY by remember { mutableFloatStateOf(250f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Draw gesture for: ${appInfo?.displayName ?: "App"}",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = "Draw any shape, letter (W, C, V, Z), or symbol in a single stroke",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Drawing workspace card with live tracking Eye
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Safe 2D Cybernetic Eye that watches finger trace safely
                SafeCyberneticEyeTracker(
                    touchX = lastPointerX,
                    touchY = lastPointerY,
                    modifier = Modifier
                        .size(54.dp)
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                )

                // Background guide watermark
                if (points.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Gesture,
                            contentDescription = null,
                            tint = CyanNeonPrimary.copy(alpha = 0.35f),
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "Draw your symbol here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                }

                // Drawing Canvas
                GestureDrawingCanvas(
                    captureController = captureController,
                    onPointerMove = { px, py ->
                        lastPointerX = px
                        lastPointerY = py
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Stroke status pill
                if (points.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
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
                                        if (points.size >= 4) CyanNeonPrimary else Color(0xFFFBBF24),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (points.size >= 4) "Gesture captured (${points.size} points)" else "Points: ${points.size} (too short)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (points.size >= 4) CyanNeonPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom action controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear", color = TextPrimary)
            }

            Button(
                onClick = {
                    val currentPoints = captureController.points.value
                    onSave(currentPoints)
                },
                enabled = points.size >= 4,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeonPrimary,
                    contentColor = CyanOnPrimary,
                    disabledContainerColor = SurfaceContainerHigh,
                    disabledContentColor = TextTertiary
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save & Activate Gesture", color = if (points.size >= 4) CyanOnPrimary else TextTertiary)
            }
        }
    }
}
