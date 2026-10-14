package com.example.floatingeye.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.floatingeye.shortcuts.AppShortcut
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FloatingAppShortcutWindow(
    shortcut: AppShortcut,
    isMinimized: Boolean,
    onRestore: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isMinimized) {
        Surface(
            onClick = onRestore,
            shape = CircleShape,
            color = Color(0xFF0C121D),
            border = androidx.compose.foundation.BorderStroke(2.dp, CyanNeonPrimary),
            shadowElevation = 12.dp,
            modifier = modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Android, contentDescription = "Restore ${shortcut.displayName}", tint = CyanNeonPrimary)
            }
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF20C121D),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, CyanNeonPrimary.copy(alpha = 0.7f)),
        shadowElevation = 18.dp,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onDrag(drag.x, drag.y)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = shortcut.displayName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onMinimize, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Minimize, contentDescription = "Minimize", tint = TextSecondary)
                }
                IconButton(onClick = onMaximize, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = "Maximize ${shortcut.displayName}", tint = CyanNeonPrimary)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close panel", tint = TextSecondary)
                }
            }

            Column(
                modifier = Modifier.weight(2f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10243A),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeonPrimary.copy(alpha = 0.65f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Android, contentDescription = null, tint = CyanNeonPrimary, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(shortcut.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "App la pare dèyè panno sa a. Peze Maximize pou louvri l nòmalman.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
