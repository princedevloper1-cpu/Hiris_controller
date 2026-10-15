package com.example.floatingeye.ui

import android.graphics.BitmapFactory
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.notifications.CapturedNotification
import com.example.floatingeye.notifications.NotificationHistoryRepository
import com.example.floatingeye.notifications.NotificationPermissionHelper
import com.example.floatingeye.notifications.RetentionPeriod
import com.example.floatingeye.shortcuts.DrawableAppIcon
import com.example.floatingeye.shortcuts.InstalledAppsRepository
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
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CyberNotificationHistoryCard(
    repository: NotificationHistoryRepository,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onCreateReminder: ((CapturedNotification) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notifications by repository.notifications.collectAsStateWithLifecycle()
    val isCaptureEnabled by repository.isCaptureEnabled.collectAsStateWithLifecycle()
    val selectedRetention by repository.selectedRetention.collectAsStateWithLifecycle()

    var isCardExpanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }

    var hasPermission by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationAccessGranted(context))
    }

    // Refresh permission when user returns from system Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = NotificationPermissionHelper.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val installedAppsRepo = remember { InstalledAppsRepository(context) }

    var searchQuery by remember { mutableStateOf("") }
    var showOnlyMedia by remember { mutableStateOf(false) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var selectedConversationKey by remember { mutableStateOf<String?>(null) }
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Filter notifications
    val filteredNotifications = remember(notifications, searchQuery, showOnlyMedia) {
        notifications.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.message.contains(searchQuery, ignoreCase = true) ||
                    item.appName.contains(searchQuery, ignoreCase = true)

            val matchesMedia = !showOnlyMedia || item.isMediaCaptured

            matchesSearch && matchesMedia
        }
    }

    val appGroups = remember(filteredNotifications) {
        filteredNotifications.groupBy { it.packageName }
            .mapValues { (_, items) -> items.sortedByDescending { it.timestamp } }
            .toList()
            .sortedByDescending { (_, items) -> items.firstOrNull()?.timestamp ?: 0L }
    }
    val selectedAppNotifications = remember(filteredNotifications, selectedAppPackage) {
        filteredNotifications.filter { it.packageName == selectedAppPackage }
            .sortedByDescending { it.timestamp }
    }
    val conversationGroups = remember(selectedAppNotifications) {
        selectedAppNotifications.groupBy { it.conversationKey() }
            .mapValues { (_, items) -> items.sortedByDescending { it.timestamp } }
            .toList()
            .sortedByDescending { (_, items) -> items.firstOrNull()?.timestamp ?: 0L }
    }
    val selectedConversationNotifications = remember(selectedAppNotifications, selectedConversationKey) {
        selectedAppNotifications.filter { it.conversationKey() == selectedConversationKey }
            .sortedBy { it.timestamp }
    }

    val mediaCount = remember(notifications) {
        notifications.count { it.isMediaCaptured }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerLow,
        border = BorderStroke(1.5.dp, if (isCaptureEnabled && hasPermission) CyanNeonPrimary.copy(alpha = 0.5f) else Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cyber_notification_history_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCaptureEnabled && hasPermission) CyanPrimaryContainer else SurfaceContainerHigh,
                        border = BorderStroke(1.dp, if (isCaptureEnabled && hasPermission) CyanNeonPrimary else Color.Transparent),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCaptureEnabled && hasPermission) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = if (isCaptureEnabled && hasPermission) CyanNeonPrimary else TextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Notification & Media Vault",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Preserve messages & media even if deleted",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCaptureEnabled && hasPermission) CyanNeonPrimary else TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isCaptureEnabled,
                        onCheckedChange = { enabled ->
                            repository.setCaptureEnabled(enabled)
                            if (enabled && !hasPermission) {
                                NotificationPermissionHelper.openNotificationListenerSettings(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeonPrimary,
                            checkedTrackColor = CyanPrimaryContainer,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = SurfaceContainerHigh
                        ),
                        modifier = Modifier.testTag("switch_capture_notifications")
                    )

                    // Space-reduction Chip
                    FilterChip(
                        selected = isCardExpanded,
                        onClick = { isCardExpanded = !isCardExpanded },
                        label = {
                            Text(
                                text = if (isCardExpanded) "Collapse" else "Expand (${notifications.size})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCardExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
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
                        border = BorderStroke(
                            1.dp,
                            if (isCardExpanded) CyanNeonPrimary else Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("chip_notif_collapse")
                    )
                }
            }

            // Compact Mode row when collapsed to save space
            if (!isCardExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCardExpanded = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark
                        ) {
                            Text(
                                text = "${notifications.size} saved messages",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeonPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (mediaCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF003D20)
                            ) {
                                Text(
                                    text = "$mediaCount photos/media",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38E07B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceContainerHigh
                    ) {
                        Text(
                            text = "Retention: ${selectedRetention.code}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Expanded Mode details
            AnimatedVisibility(visible = isCardExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Permission Warning Banner if not granted
                    if (!hasPermission) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2D1500),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Notification Access Required",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        Text(
                            text = "Allow notification access to save notifications and message media.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFDE68A).copy(alpha = 0.9f)
                        )
                        Button(
                            onClick = {
                                NotificationPermissionHelper.openNotificationListenerSettings(context)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_grant_notification_access")
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GRANT ACCESS IN SETTINGS",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Section: Retention Period Setting (8h, 24h, 1week, Never)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoDelete,
                                contentDescription = null,
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Auto-Delete Retention",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = selectedRetention.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Choose how long saved messages and media are kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // 4 Retention Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RetentionPeriod.entries.forEach { period ->
                            val isSelected = selectedRetention == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { repository.setRetentionPeriod(period) },
                                label = {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeonPrimary,
                                    selectedLabelColor = CyanOnPrimary,
                                    selectedLeadingIconColor = CyanOnPrimary,
                                    containerColor = SurfaceDark,
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanNeonPrimary else Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("chip_retention_${period.code}")
                            )
                        }
                    }
                }
            }

            // Stats row & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark
                    ) {
                        Text(
                            text = "${notifications.size} messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (mediaCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF003D20)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, tint = Color(0xFF38E07B), modifier = Modifier.size(12.dp))
                                Text(
                                    text = "$mediaCount photos/media",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38E07B)
                                )
                            }
                        }
                    }
                }

                if (notifications.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Clear All", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Search Bar & Filter Options
            if (notifications.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by sender or message...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeonPrimary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        cursorColor = CyanNeonPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("input_search_notifications")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Inbox", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    if (mediaCount > 0) {
                        FilterChip(
                            selected = showOnlyMedia,
                            onClick = { showOnlyMedia = !showOnlyMedia },
                            leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            label = { Text(if (showOnlyMedia) "Media only" else "All", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeonPrimary,
                                selectedLabelColor = CyanOnPrimary,
                                containerColor = SurfaceDark,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // App → conversation → captured message navigation
            if (filteredNotifications.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (notifications.isEmpty()) "No notifications captured yet" else "No messages match your search",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (notifications.isEmpty())
                                "Incoming notifications from WhatsApp, Messenger, and messaging apps will be backed up here with their photos."
                            else
                                "Try adjusting your search terms or clearing active filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (selectedAppPackage == null) {
                Text("Choose an app", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    appGroups.forEach { (packageName, appItems) ->
                        NotificationAppRow(
                            packageName = packageName,
                            appName = appItems.first().appName,
                            notifications = appItems,
                            installedAppsRepo = installedAppsRepo,
                            onClick = {
                                selectedAppPackage = packageName
                                selectedConversationKey = null
                            }
                        )
                    }
                }
            } else if (selectedConversationKey == null) {
                val appName = selectedAppNotifications.firstOrNull()?.appName ?: selectedAppPackage.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { selectedAppPackage = null }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to apps", tint = TextPrimary)
                    }
                    Column {
                        Text(appName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${conversationGroups.size} conversations", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                if (conversationGroups.isEmpty()) {
                    Text("No recent notifications for this app.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        conversationGroups.forEach { (conversationKey, messages) ->
                            NotificationConversationRow(
                                senderName = messages.first().conversationDisplayName(),
                                messages = messages,
                                onClick = { selectedConversationKey = conversationKey }
                            )
                        }
                    }
                }
            } else {
                val appName = selectedAppNotifications.firstOrNull()?.appName ?: selectedAppPackage.orEmpty()
                val senderName = selectedConversationNotifications.firstOrNull()?.conversationDisplayName() ?: selectedConversationKey.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { selectedConversationKey = null }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to conversations", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(senderName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("$appName · ${selectedConversationNotifications.size} messages", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.AutoDelete, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                        Text("Retention: ${selectedRetention.label}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(selectedConversationNotifications, key = { it.id }) { notification ->
                        NotificationMessageRow(
                            notification = notification,
                            senderName = notification.conversationDisplayName(),
                            onViewPhoto = { path -> viewingPhotoPath = path },
                            onDelete = { repository.deleteNotification(notification.id) },
                            onCreateReminder = { onCreateReminder?.invoke(notification) }
                        )
                    }
                }
            }
        }
    }
    }
}

    // Photo Full-Screen Viewer Dialog
    viewingPhotoPath?.let { photoPath ->
        Dialog(onDismissRequest = { viewingPhotoPath = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.5.dp, CyanNeonPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saved Photo (Local Vault)",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyanNeonPrimary
                        )
                        IconButton(
                            onClick = { viewingPhotoPath = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    val bitmap = remember(photoPath) {
                        try {
                            BitmapFactory.decodeFile(photoPath)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Full Image Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text(
                            text = "Image file unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = "This photo is preserved in Hiris Controller local storage. It remains saved even if deleted from chat.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear entire notification history?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all ${notifications.size} saved notifications and all captured media files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.clearAll()
                        selectedAppPackage = null
                        selectedConversationKey = null
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

private fun CapturedNotification.conversationKey(): String =
    (senderName?.takeIf { it.isNotBlank() } ?: title.ifBlank { "Unknown sender" }).trim().lowercase()

private fun CapturedNotification.conversationDisplayName(): String =
    (senderName?.takeIf { it.isNotBlank() } ?: title.ifBlank { "Unknown sender" }).trim()

@Composable
private fun NotificationAppRow(
    packageName: String,
    appName: String,
    notifications: List<CapturedNotification>,
    installedAppsRepo: InstalledAppsRepository,
    onClick: () -> Unit
) {
    val icon = remember(packageName) { installedAppsRepo.getAppIcon(packageName) }
    val latest = notifications.firstOrNull()
    val conversationsCount = notifications.map { it.conversationKey() }.distinct().size
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DrawableAppIcon(icon, appName, Modifier.size(44.dp).clip(CircleShape))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$conversationsCount conversations · ${notifications.size} notifications", style = MaterialTheme.typography.labelSmall, color = CyanNeonPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(latest?.let { it.message.ifBlank { it.title } }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            latest?.let {
                Text(
                    DateUtils.getRelativeTimeSpanString(it.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun NotificationConversationRow(
    senderName: String,
    messages: List<CapturedNotification>,
    onClick: () -> Unit
) {
    val latest = messages.firstOrNull()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = CyanNeonPrimary.copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(senderName.firstOrNull()?.uppercase() ?: "?", color = CyanNeonPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(senderName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${messages.size} ${if (messages.size == 1) "message" else "messages"}", style = MaterialTheme.typography.labelSmall, color = CyanNeonPrimary)
                Text(latest?.let { it.message.ifBlank { it.title } }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            latest?.let {
                Text(
                    DateUtils.getRelativeTimeSpanString(it.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun NotificationMessageRow(
    notification: CapturedNotification,
    senderName: String,
    onViewPhoto: (String) -> Unit,
    onDelete: () -> Unit,
    onCreateReminder: (() -> Unit)?
) {
    val context = LocalContext.current
    var dragOffset by remember(notification.id) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val actionWidth = with(density) { (if (onCreateReminder != null) 160.dp else 80.dp).toPx() }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCreateReminder != null) {
                Surface(
                    color = Color(0xFF1D4ED8), shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(80.dp).heightIn(min = 76.dp).clickable { onCreateReminder(); dragOffset = 0f }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = Color.White)
                        Text("Reminder", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Surface(
                color = Color(0xFF7F1D1D), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(80.dp).heightIn(min = 76.dp).clickable { onDelete(); dragOffset = 0f }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color.White)
                    Text("Delete", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().offset { IntOffset(dragOffset.roundToInt(), 0) }
            .pointerInput(notification.id, onCreateReminder) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount).coerceIn(-actionWidth, 0f)
                    },
                    onDragEnd = { dragOffset = if (dragOffset < -actionWidth / 2f) -actionWidth else 0f }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(senderName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = CyanNeonPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(DateUtils.formatDateTime(context, notification.timestamp, DateUtils.FORMAT_SHOW_TIME), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
            Text(notification.message.ifBlank { notification.title }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            notification.mediaPath?.takeIf { it.isNotBlank() }?.let { mediaPath ->
                TextButton(onClick = { onViewPhoto(mediaPath) }) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanNeonPrimary)
                    Spacer(Modifier.width(5.dp))
                    Text("View saved attachment", color = CyanNeonPrimary)
                }
            }
        }
    }
    }
}

@Composable
private fun CapturedNotificationItem(
    notification: CapturedNotification,
    installedAppsRepo: InstalledAppsRepository,
    onViewPhoto: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: App Icon, App Name, Time, and Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // App Icon
                    val iconDrawable = remember(notification.packageName) {
                        installedAppsRepo.getAppIcon(notification.packageName)
                    }
                    DrawableAppIcon(
                        drawable = iconDrawable,
                        contentDescription = notification.appName,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Text(
                        text = notification.appName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeonPrimary
                    )

                    // Preserved / Protected Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF003D20)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF38E07B), modifier = Modifier.size(10.dp))
                            Text(
                                text = "SAVED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF38E07B)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val relativeTime = remember(notification.timestamp) {
                        DateUtils.getRelativeTimeSpanString(
                            notification.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    }
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Delete",
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Row 2: Title (Sender / Group name)
            if (notification.title.isNotEmpty()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            // Row 3: Message Text
            if (notification.message.isNotEmpty()) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Row 4: Photo / Media attachment if captured!
            if (!notification.mediaPath.isNullOrEmpty()) {
                val bitmap = remember(notification.mediaPath) {
                    try {
                        BitmapFactory.decodeFile(notification.mediaPath)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onViewPhoto(notification.mediaPath) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Captured Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Tap to enlarge badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceDark.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Image, contentDescription = null, tint = CyanNeonPrimary, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Tap to enlarge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanNeonPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
