package com.example.floatingeye.ui.downloader

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.downloader.model.DownloadStatus
import com.example.floatingeye.downloader.model.MediaDownloadItem
import com.example.floatingeye.downloader.model.MediaType
import com.example.floatingeye.downloader.repository.MediaDownloadRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CyberMediaDownloaderCard(
    repository: MediaDownloadRepository,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val downloads by repository.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MediaType.VIDEO) }
    var selectedQuality by remember { mutableStateOf("1080p / MP4") }

    // Auto-detect URL from clipboard on open
    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0)?.text?.toString()?.trim() ?: ""
                    if ((text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) && urlInput.isBlank()) {
                        urlInput = text
                        Toast.makeText(context, "Lyen detekte nan Clipboard!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanNeonPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Downloader",
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Media Downloader",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Telechaje Videyo & Odyo ak lyen dirèk",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // URL Input Box with Paste Button
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Kole lyen videyo / odyo a (HTTP / HTTPS)") },
                placeholder = { Text("Lyen dirèk pou fichye .mp4 oswa .mp3") },
                singleLine = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (urlInput.isNotBlank()) {
                            IconButton(onClick = { urlInput = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val item = clipboard?.primaryClip?.getItemAt(0)
                                    val text = item?.text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        urlInput = text
                                        Toast.makeText(context, "Kole!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = CyanNeonPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeonPrimary,
                    unfocusedBorderColor = TextTertiary,
                    focusedLabelColor = CyanNeonPrimary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Sèvi ak lyen dirèk .mp3 oswa .mp4. Lyen paj YouTube/Instagram ak konvèsyon ant fòma pa sipòte.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Title (Optional)
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Title / File Name (Optional)") },
                placeholder = { Text("e.g., Video title or podcast...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeonPrimary,
                    unfocusedBorderColor = TextTertiary,
                    focusedLabelColor = CyanNeonPrimary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Format Selection: Video (MP4) vs Audio (MP3)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedType == MediaType.VIDEO,
                    onClick = {
                        selectedType = MediaType.VIDEO
                        selectedQuality = "1080p / MP4"
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Videyo (MP4)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeonPrimary.copy(alpha = 0.25f),
                        selectedLabelColor = CyanNeonPrimary,
                        selectedLeadingIconColor = CyanNeonPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedType == MediaType.AUDIO,
                    onClick = {
                        selectedType = MediaType.AUDIO
                        selectedQuality = "320kbps / MP3"
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Odyo (MP3)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFA855F7).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFFA855F7),
                        selectedLeadingIconColor = Color(0xFFA855F7)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Download Trigger Button
            Button(
                onClick = {
                    if (urlInput.isBlank()) {
                        Toast.makeText(context, "Tanpri antre oswa kole yon lyen!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val result = repository.startDownload(
                        url = urlInput,
                        customTitle = titleInput.takeIf { it.isNotBlank() },
                        type = selectedType,
                        quality = selectedQuality
                    )
                    result.onSuccess {
                        Toast.makeText(context, "Telechajman kòmanse nan background!", Toast.LENGTH_SHORT).show()
                        urlInput = ""
                        titleInput = ""
                    }.onFailure { err ->
                        Toast.makeText(context, "Erè: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedType == MediaType.VIDEO) "Telechaje Videyo (MP4)" else "Telechaje Odyo (MP3)",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Download History / Active List
            Text(
                text = "Telechajman ki fèt & an kour (${downloads.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (downloads.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainerHigh.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No downloaded files yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Paste a link above to start downloading.", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    downloads.forEach { item ->
                        DownloadItemRow(
                            item = item,
                            onDelete = {
                                repository.removeDownload(item.id)
                                Toast.makeText(context, "Item removed from download list", Toast.LENGTH_SHORT).show()
                            },
                            onOpen = {
                                try {
                                    val fileUri = repository.getDownloadedFileUri(item)
                                    if (fileUri == null) {
                                        Toast.makeText(context, "Fichye a poko telechaje oswa Android pa jwenn li.", Toast.LENGTH_SHORT).show()
                                        return@DownloadItemRow
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            fileUri,
                                            if (item.mediaType == MediaType.AUDIO) "audio/mpeg" else "video/mp4"
                                        )
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Pa gen app sou telefòn nan ki ka louvri fichye sa a.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShare = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, item.title)
                                        putExtra(Intent.EXTRA_TEXT, "${item.title}: ${item.sourceUrl}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Media Link"))
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItemRow(
    item: MediaDownloadItem,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            when (item.status) {
                DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.4f)
                DownloadStatus.DOWNLOADING -> CyanNeonPrimary.copy(alpha = 0.5f)
                DownloadStatus.FAILED -> Color(0xFFEF4444).copy(alpha = 0.5f)
                DownloadStatus.PENDING -> Color(0xFFFFB020).copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Media Type Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (item.mediaType == MediaType.VIDEO) CyanNeonPrimary.copy(alpha = 0.2f)
                            else Color(0xFFA855F7).copy(alpha = 0.2f)
                        )
                ) {
                    Icon(
                        imageVector = if (item.mediaType == MediaType.VIDEO) Icons.Rounded.Movie else Icons.Rounded.Audiotrack,
                        contentDescription = null,
                        tint = if (item.mediaType == MediaType.VIDEO) CyanNeonPrimary else Color(0xFFA855F7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (item.mediaType == MediaType.VIDEO) "MP4" else "MP3",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.mediaType == MediaType.VIDEO) CyanNeonPrimary else Color(0xFFA855F7)
                        )
                        Text(text = "•", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = when (item.status) {
                                DownloadStatus.COMPLETED -> "Completed"
                                DownloadStatus.DOWNLOADING -> "Downloading..."
                                DownloadStatus.FAILED -> "Failed"
                                DownloadStatus.PENDING -> "Pending"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (item.status) {
                                DownloadStatus.COMPLETED -> Color(0xFF10B981)
                                DownloadStatus.DOWNLOADING -> CyanNeonPrimary
                                DownloadStatus.FAILED -> Color(0xFFEF4444)
                                DownloadStatus.PENDING -> Color(0xFFFFB020)
                            }
                        )
                    }
                }

                // Action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onOpen, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play/Open", tint = CyanNeonPrimary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Progress bar if DOWNLOADING
            if (item.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progressPercent },
                    color = CyanNeonPrimary,
                    trackColor = Color(0xFF1E2D48),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                )
            }
            if (item.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFCA5A5)
                )
            }
        }
    }
}
