package com.example.floatingeye.shortcuts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DrawableAppIcon(
    packageName: String,
    installedAppsRepository: InstalledAppsRepository,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val icon = remember(packageName) { installedAppsRepository.getAppIcon(packageName) }
    DrawableAppIcon(
        drawable = icon,
        contentDescription = packageName,
        modifier = modifier.size(size)
    )
}

@Composable
fun DrawableAppIcon(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        drawable?.let { d ->
            try {
                val w = if (d.intrinsicWidth > 0) d.intrinsicWidth.coerceIn(48, 128) else 96
                val h = if (d.intrinsicHeight > 0) d.intrinsicHeight.coerceIn(48, 128) else 96
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                d.setBounds(0, 0, canvas.width, canvas.height)
                d.draw(canvas)
                bmp.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(SurfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = contentDescription,
                tint = CyanNeonPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShortcutPicker(
    installedAppsRepository: InstalledAppsRepository,
    selectedShortcuts: List<AppShortcut>,
    maxSelectable: Int = AppShortcutRepository.MAX_SHORTCUTS,
    onToggleApp: (InstalledAppInfo, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = installedAppsRepository.getInstalledLaunchableApps()
        isLoading = false
    }

    val selectedPackageNames = remember(selectedShortcuts) {
        selectedShortcuts.map { it.packageName }.toSet()
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceContainerHigh,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add App to Floating Eye",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${selectedShortcuts.size} of ${maxSelectable.coerceAtMost(AppShortcutRepository.MAX_SHORTCUTS)} app slots available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedShortcuts.size >= maxSelectable) CyanNeonPrimary else TextSecondary
                    )
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

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_apps"),
                placeholder = {
                    Text("Search installed apps...", color = TextTertiary)
                },
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
                    unfocusedContainerColor = SurfaceContainerLow,
                    cursorColor = CyanNeonPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanNeonPrimary)
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No installed apps found" else "No apps matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appInfo ->
                        val isSelected = selectedPackageNames.contains(appInfo.packageName)
                        val canSelect = isSelected || selectedShortcuts.size < maxSelectable

                        AppPickerRow(
                            appInfo = appInfo,
                            isSelected = isSelected,
                            canSelect = canSelect,
                            onToggle = {
                                onToggleApp(appInfo, !isSelected)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppPickerRow(
    appInfo: InstalledAppInfo,
    isSelected: Boolean,
    canSelect: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) CyanPrimaryContainer.copy(alpha = 0.35f) else SurfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
                            .clickable {
                                if (canSelect) {
                                    onToggle()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Floating Eye ka montre 8 app ak extension sèlman. Retire youn anvan ou ajoute yon lòt.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
            .testTag("app_row_${appInfo.packageName}")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Icon: 32dp inside 44dp rounded container
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

            // App Name & Package Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canSelect) TextPrimary else TextTertiary
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            // Checkmark indicator
            Surface(
                shape = CircleShape,
                color = if (isSelected) CyanNeonPrimary else SurfaceContainerHigh,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = SurfaceContainerLow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
