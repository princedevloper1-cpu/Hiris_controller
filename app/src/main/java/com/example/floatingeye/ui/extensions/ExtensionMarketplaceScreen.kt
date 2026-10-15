package com.example.floatingeye.ui.extensions

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.auth.HirisAuth
import com.example.floatingeye.extensions.market.data.HirisExtensionsApi
import com.example.floatingeye.extensions.market.model.MarketplaceExtension
import com.example.floatingeye.extensions.market.model.MarketplaceUploadFile
import com.example.floatingeye.extensions.market.repository.ExtensionMarketplaceRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun ExtensionMarketplaceScreen(onBackToInstalled: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { ExtensionMarketplaceRepository.getInstance(context) }
    var extensions by remember { mutableStateOf<List<MarketplaceExtension>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var editingExtension by remember { mutableStateOf<MarketplaceExtension?>(null) }
    var deletingExtension by remember { mutableStateOf<MarketplaceExtension?>(null) }
    var selectedFile by remember { mutableStateOf<MarketplaceUploadFile?>(null) }

    suspend fun refresh() {
        val token = HirisAuth.getToken(context)
        if (token.isNullOrBlank()) {
            extensions = emptyList()
            errorMessage = "Log in from Profile to browse the extension market."
            return
        }
        isLoading = true
        errorMessage = null
        try {
            extensions = repository.list(token)
        } catch (error: Exception) {
            errorMessage = error.message ?: "Could not load extensions. Check your connection."
        } finally {
            isLoading = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                errorMessage = null
                try {
                    selectedFile = withContext(Dispatchers.IO) { readUploadFile(context, uri) }
                } catch (error: Exception) {
                    errorMessage = error.message ?: "Could not read the selected file."
                } finally {
                    isWorking = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToInstalled) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to extensions", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Extension Market", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Browse and manage packages", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { scope.launch { refresh() } }, enabled = !isLoading && !isWorking) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh market", tint = CyanNeonPrimary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF101B2B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C2C46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Downloaded extensions stay in HIRIS Controller > My extensions on this phone. HIRIS does not run package files as code.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                editingExtension = null
                                selectedFile = null
                                showUploadDialog = true
                            },
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Add extension (admin)")
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = CyanNeonPrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(10.dp))
                        Text("Loading extensions…", color = TextSecondary)
                    }
                }
            }

            errorMessage?.let { message ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2B1820),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D35)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(message, color = Color(0xFFFCA5A5), modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (!isLoading && extensions.isEmpty() && errorMessage == null) {
                item { Text("No extensions in the market yet.", color = TextTertiary, modifier = Modifier.padding(12.dp)) }
            }

            items(extensions, key = { it.id }) { extension ->
                MarketplaceExtensionCard(
                    extension = extension,
                    isWorking = isWorking,
                    onDownload = {
                        val token = HirisAuth.getToken(context)
                        if (token.isNullOrBlank()) {
                            errorMessage = "Log in from Profile to download extensions."
                        } else {
                            scope.launch {
                                isWorking = true
                                errorMessage = null
                                try {
                                    val result = repository.download(token, extension.id)
                                    Toast.makeText(
                                        context,
                                        "Saved ${result.filename} to ${result.savedLocation}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    refresh()
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Download failed."
                                } finally {
                                    isWorking = false
                                }
                            }
                        }
                    },
                    onEdit = {
                        selectedFile = null
                        editingExtension = extension
                    },
                    onDelete = { deletingExtension = extension }
                )
            }
        }
    }

    if (showUploadDialog || editingExtension != null) {
        MarketplaceExtensionForm(
            extension = editingExtension,
            selectedFile = selectedFile,
            isWorking = isWorking,
            onChooseFile = { filePicker.launch(arrayOf("*/*")) },
            onDismiss = {
                showUploadDialog = false
                editingExtension = null
                selectedFile = null
            },
            onSubmit = { name, description, version ->
                val token = HirisAuth.getToken(context)
                if (token.isNullOrBlank()) {
                    errorMessage = "Log in from Profile to manage extensions."
                } else {
                    val editing = editingExtension
                    val file = selectedFile
                    if (editing == null && file == null) {
                        errorMessage = "Choose an extension package before uploading."
                    } else {
                        scope.launch {
                            isWorking = true
                            errorMessage = null
                            try {
                                if (editing == null) {
                                    repository.upload(token, name, description, version, requireNotNull(file))
                                } else {
                                    repository.update(token, editing.id, name, description, version, file)
                                }
                                showUploadDialog = false
                                editingExtension = null
                                selectedFile = null
                                refresh()
                            } catch (error: Exception) {
                                errorMessage = error.message ?: "Could not save the extension."
                            } finally {
                                isWorking = false
                            }
                        }
                    }
                }
            }
        )
    }

    deletingExtension?.let { extension ->
        AlertDialog(
            onDismissRequest = { deletingExtension = null },
            title = { Text("Delete extension?") },
            text = { Text("Delete ${extension.name} from the market? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        val token = HirisAuth.getToken(context)
                        if (token.isNullOrBlank()) {
                            errorMessage = "Log in from Profile to manage extensions."
                            deletingExtension = null
                        } else {
                            scope.launch {
                                isWorking = true
                                errorMessage = null
                                try {
                                    repository.delete(token, extension.id)
                                    deletingExtension = null
                                    refresh()
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Could not delete the extension."
                                } finally {
                                    isWorking = false
                                }
                            }
                        }
                    }
                ) { Text("Delete", color = Color(0xFFF87171)) }
            },
            dismissButton = { TextButton(onClick = { deletingExtension = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MarketplaceExtensionCard(
    extension: MarketplaceExtension,
    isWorking: Boolean,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E1524),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C2C46)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(extension.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Version ${extension.version}", color = CyanNeonPrimary, style = MaterialTheme.typography.labelSmall)
                }
                Text(formatPackageSize(extension.size), color = TextTertiary, style = MaterialTheme.typography.labelSmall)
            }
            if (extension.description.isNotBlank()) {
                Text(extension.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                listOfNotNull(extension.author.takeIf(String::isNotBlank), "${extension.downloads} downloads")
                    .joinToString(" · "),
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onDownload, enabled = !isWorking, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Download")
                }
                IconButton(onClick = onEdit, enabled = !isWorking) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit ${extension.name}", tint = CyanNeonPrimary)
                }
                IconButton(onClick = onDelete, enabled = !isWorking) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${extension.name}", tint = Color(0xFFF87171))
                }
            }
        }
    }
}

@Composable
private fun MarketplaceExtensionForm(
    extension: MarketplaceExtension?,
    selectedFile: MarketplaceUploadFile?,
    isWorking: Boolean,
    onChooseFile: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var name by remember(extension?.id) { mutableStateOf(extension?.name.orEmpty()) }
    var description by remember(extension?.id) { mutableStateOf(extension?.description.orEmpty()) }
    var version by remember(extension?.id) { mutableStateOf(extension?.version.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (extension == null) "Add extension" else "Update extension") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
                OutlinedTextField(
                    version,
                    { version = it },
                    label = { Text("Version") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                OutlinedButton(onClick = onChooseFile, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(selectedFile?.filename ?: if (extension == null) "Choose package (max 5 MB)" else "Replace package (optional)")
                }
                if (selectedFile != null) {
                    Text("${selectedFile.filename} · ${formatPackageSize(selectedFile.bytes.size.toLong())}", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isWorking && name.isNotBlank() && version.isNotBlank() && (extension != null || selectedFile != null),
                onClick = { onSubmit(name, description, version) }
            ) {
                if (isWorking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (extension == null) "Upload" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") } }
    )
}

private fun readUploadFile(context: Context, uri: Uri): MarketplaceUploadFile {
    val resolver = context.contentResolver
    val filename = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> cursor.firstString(OpenableColumns.DISPLAY_NAME) }
        ?.takeIf(String::isNotBlank)
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "extension.bin"
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= HirisExtensionsApi.MAX_FILE_BYTES) { "Extension files must be 5 MB or smaller." }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: throw IllegalStateException("Could not open the selected file.")
    require(bytes.isNotEmpty()) { "The selected file is empty." }
    return MarketplaceUploadFile(
        bytes = bytes,
        filename = filename.substringAfterLast('/').substringAfterLast('\\'),
        contentType = resolver.getType(uri) ?: "application/octet-stream"
    )
}

private fun Cursor.firstString(columnName: String): String? {
    if (!moveToFirst()) return null
    val column = getColumnIndex(columnName)
    return if (column >= 0) getString(column) else null
}

private fun formatPackageSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
