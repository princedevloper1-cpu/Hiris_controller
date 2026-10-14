package com.example.floatingeye.ui.firewall

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.firewall.model.ActiveNetworkType
import com.example.floatingeye.firewall.model.FirewallAppRule
import com.example.floatingeye.firewall.repository.FirewallRepository
import com.example.floatingeye.shortcuts.InstalledAppInfo
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val NeonAmber = Color(0xFFF59E0B)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CyberFirewallCard(
    repository: FirewallRepository,
    installedAppsRepository: InstalledAppsRepository? = null,
    onFirewallActiveChanged: (Boolean) -> Unit,
    onRequestFirewallEnable: () -> Unit,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFirewallActive by repository.isFirewallActive.collectAsStateWithLifecycle()
    val rules by repository.rules.collectAsStateWithLifecycle()
    val activeNetwork by repository.activeNetworkType.collectAsStateWithLifecycle()

    val effectiveAppsRepo = remember(context, installedAppsRepository) {
        installedAppsRepository ?: InstalledAppsRepository(context)
    }

    // Load installed apps asynchronously
    val installedApps by produceState<List<InstalledAppInfo>>(initialValue = emptyList()) {
        value = effectiveAppsRepo.getInstalledLaunchableApps()
    }

    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val blockedCount = rules.values.count { it.isAnyBlocked }

    val requestEnableFirewall = {
        onRequestFirewallEnable()
    }

    val networkLabel = when (activeNetwork) {
        ActiveNetworkType.WIFI -> "Wi-Fi Connected"
        ActiveNetworkType.MOBILE -> "Mobile Data (Cellular)"
        ActiveNetworkType.NONE -> "No Active Network"
    }

    val networkColor = when (activeNetwork) {
        ActiveNetworkType.WIFI -> CyanNeonPrimary
        ActiveNetworkType.MOBILE -> NeonAmber
        ActiveNetworkType.NONE -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(
            1.2.dp,
            if (isFirewallActive) CyanNeonPrimary.copy(alpha = 0.6f) else Color(0xFF1E293B)
        ),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Bar: Icon, Title, Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFirewallActive)
                                    Brush.radialGradient(listOf(CyanNeonPrimary, Color(0xFF0284C7)))
                                else
                                    Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFirewallActive) Icons.Rounded.Shield else Icons.Rounded.Security,
                            contentDescription = "Firewall Status",
                            tint = if (isFirewallActive) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Firewall & App Blocker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(networkColor)
                            )
                            Text(
                                text = networkLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = networkColor
                            )
                        }
                    }
                }

                // Master Toggle
                Switch(
                    checked = isFirewallActive,
                    onCheckedChange = { active ->
                        if (active) {
                            requestEnableFirewall()
                        } else {
                            onFirewallActiveChanged(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanNeonPrimary,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isFirewallActive) Color(0xFF03223F) else Color(0xFF1E293B),
                border = BorderStroke(
                    1.dp,
                    if (isFirewallActive) CyanNeonPrimary.copy(alpha = 0.35f) else Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isFirewallActive) "SHIELD ACTIVE" else "FIREWALL PAUSED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFirewallActive) CyanNeonPrimary else TextSecondary
                        )
                        Text(
                            text = if (blockedCount == 0)
                                "No apps restricted yet. Select apps below."
                            else
                                "$blockedCount app(s) isolated from internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (isFirewallActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ISOLATING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeonPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Preset Bar (WhatsApp, Facebook, Instagram, etc.)
            Text(
                text = "POPULAR APPS (QUICK ONE-TAP BLOCK)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CyanNeonPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FirewallRepository.POPULAR_APPS.forEach { popRule ->
                    val isBlocked = rules[popRule.packageName]?.isAnyBlocked ?: false
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isBlocked) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isBlocked) Color(0xFFEF4444) else Color(0xFF334155)
                        ),
                        modifier = Modifier.clickable {
                            if (!isFirewallActive) {
                                requestEnableFirewall()
                            }
                            repository.toggleBlockAll(popRule.packageName, popRule.appName)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isBlocked) Icons.Rounded.Block else Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (isBlocked) Color(0xFFEF4444) else CyanNeonPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = popRule.appName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isBlocked) Color(0xFFFCA5A5) else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Filter Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search apps (e.g. WhatsApp, Facebook)...",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = CyanNeonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeonPrimary,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0B111E),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Installed Apps List with Wi-Fi / Mobile Data block controls
            val filteredApps = remember(installedApps, searchQuery, rules) {
                val query = searchQuery.trim().lowercase()
                val list = if (query.isEmpty()) {
                    installedApps
                } else {
                    installedApps.filter {
                        it.displayName.lowercase().contains(query) ||
                                it.packageName.lowercase().contains(query)
                    }
                }
                // Sort blocked apps first, then alphabetical
                list.sortedWith(
                    compareByDescending<InstalledAppInfo> {
                        rules[it.packageName]?.isAnyBlocked ?: false
                    }.thenBy { it.displayName.lowercase() }
                )
            }

            Text(
                text = "INSTALLED APPS (${filteredApps.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredApps.take(25).forEach { appInfo ->
                    val rule = rules[appInfo.packageName]
                    val isMobileBlocked = rule?.blockMobileData ?: false
                    val isWifiBlocked = rule?.blockWifi ?: false

                    AppFirewallRow(
                        appInfo = appInfo,
                        isMobileBlocked = isMobileBlocked,
                        isWifiBlocked = isWifiBlocked,
                        onToggleMobile = {
                            if (!isFirewallActive) requestEnableFirewall()
                            repository.toggleBlockMobile(appInfo.packageName, appInfo.displayName)
                        },
                        onToggleWifi = {
                            if (!isFirewallActive) requestEnableFirewall()
                            repository.toggleBlockWifi(appInfo.packageName, appInfo.displayName)
                        },
                        onToggleAll = {
                            if (!isFirewallActive) requestEnableFirewall()
                            repository.toggleBlockAll(appInfo.packageName, appInfo.displayName)
                        }
                    )
                }

                if (filteredApps.size > 25) {
                    Text(
                        text = "+ ${filteredApps.size - 25} more apps (use search to find specific apps)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔒 Zero-Leak Guarantee",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When an app is blocked, its internet access is isolated through HIRIS local blackhole firewall. Blocked apps cannot send or receive messages, receive background notifications, or burn data. Non-blocked apps continue at maximum speed with zero slowdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppFirewallRow(
    appInfo: InstalledAppInfo,
    isMobileBlocked: Boolean,
    isWifiBlocked: Boolean,
    onToggleMobile: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleAll: () -> Unit
) {
    val isAnyBlocked = isMobileBlocked || isWifiBlocked
    val isAllBlocked = isMobileBlocked && isWifiBlocked

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isAllBlocked) Color(0xFF450A0A).copy(alpha = 0.35f) else Color(0xFF131D2E),
        border = BorderStroke(
            1.dp,
            if (isAllBlocked) Color(0xFFEF4444).copy(alpha = 0.6f)
            else if (isAnyBlocked) NeonAmber.copy(alpha = 0.5f)
            else Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Icon & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                DrawableImage(
                    drawable = appInfo.icon,
                    contentDescription = appInfo.displayName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appInfo.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isAnyBlocked) Color.White else TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = when {
                            isAllBlocked -> "BLOCKED: ALL NETWORK"
                            isMobileBlocked -> "BLOCKED: DATA ONLY"
                            isWifiBlocked -> "BLOCKED: WI-FI ONLY"
                            else -> "UNRESTRICTED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isAllBlocked -> Color(0xFFF87171)
                            isMobileBlocked || isWifiBlocked -> NeonAmber
                            else -> Color(0xFF34D399)
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons: Wi-Fi, Mobile Data, Block All
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Wi-Fi Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isWifiBlocked) Color(0xFFDC2626) else Color(0xFF1E293B),
                    border = BorderStroke(
                        1.dp,
                        if (isWifiBlocked) Color(0xFFEF4444) else Color(0xFF334155)
                    ),
                    modifier = Modifier.clickable { onToggleWifi() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isWifiBlocked) Icons.Rounded.WifiOff else Icons.Rounded.Wifi,
                            contentDescription = "Toggle Wi-Fi Block",
                            tint = if (isWifiBlocked) Color.White else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "WI-FI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isWifiBlocked) Color.White else TextSecondary
                        )
                    }
                }

                // Mobile Data Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMobileBlocked) Color(0xFFDC2626) else Color(0xFF1E293B),
                    border = BorderStroke(
                        1.dp,
                        if (isMobileBlocked) Color(0xFFEF4444) else Color(0xFF334155)
                    ),
                    modifier = Modifier.clickable { onToggleMobile() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NetworkCell,
                            contentDescription = "Toggle Data Block",
                            tint = if (isMobileBlocked) Color.White else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isMobileBlocked) Color.White else TextSecondary
                        )
                    }
                }

                // Block All Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAllBlocked) Color(0xFF991B1B) else Color(0xFF0F172A),
                    border = BorderStroke(
                        1.dp,
                        if (isAllBlocked) Color(0xFFEF4444) else Color(0xFF334155)
                    ),
                    modifier = Modifier.clickable { onToggleAll() }
                ) {
                    Icon(
                        imageVector = if (isAllBlocked) Icons.Rounded.Block else Icons.Rounded.Shield,
                        contentDescription = "Toggle All Network",
                        tint = if (isAllBlocked) Color.White else CyanNeonPrimary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawableImage(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        drawable?.let { drawableToBitmap(it) }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = contentDescription,
                tint = CyanNeonPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}
