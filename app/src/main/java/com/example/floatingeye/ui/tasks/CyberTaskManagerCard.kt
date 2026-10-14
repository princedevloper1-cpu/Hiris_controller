package com.example.floatingeye.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.tasks.model.HirisTask
import com.example.floatingeye.tasks.model.TaskPriority
import com.example.floatingeye.tasks.model.TaskReminderDraft
import com.example.floatingeye.tasks.repository.TaskRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun CyberTaskManagerCard(
    repository: TaskRepository,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsStateWithLifecycle()
    val reminderDraft by repository.reminderDraft.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<HirisTask?>(null) }
    LaunchedEffect(reminderDraft) { if (reminderDraft != null) showCreateDialog = true }
    var filterCompleted by remember { mutableStateOf<Boolean?>(null) } // null = all, false = pending, true = completed

    val pendingCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }

    val filteredTasks = remember(tasks, filterCompleted) {
        when (filterCompleted) {
            null -> tasks
            true -> tasks.filter { it.isCompleted }
            false -> tasks.filter { !it.isCompleted }
        }
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
                            imageVector = Icons.Rounded.EventNote,
                            contentDescription = "Tasks",
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Tasks & Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "$pendingCount an atant • $completedCount konplete",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Test sound button to verify ringer immediately
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(context, com.example.floatingeye.tasks.receiver.TaskReminderReceiver::class.java).apply {
                                putExtra(com.example.floatingeye.tasks.receiver.TaskReminderReceiver.EXTRA_TASK_ID, "test_preview")
                                putExtra(com.example.floatingeye.tasks.receiver.TaskReminderReceiver.EXTRA_TASK_TITLE, "Tès Sonri & Notifikasyon")
                                putExtra(com.example.floatingeye.tasks.receiver.TaskReminderReceiver.EXTRA_TASK_DESC, "Sonri a ap sonnen kounye a!")
                            }
                            context.sendBroadcast(intent)
                            Toast.makeText(context, "Sonri ak notifikasyon deklannche!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Test Sound",
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeonPrimary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Ajoute Tach",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Task",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterCompleted == null,
                    onClick = { filterCompleted = null },
                    label = { Text("Tout (${tasks.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeonPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanNeonPrimary
                    )
                )
                FilterChip(
                    selected = filterCompleted == false,
                    onClick = { filterCompleted = false },
                    label = { Text("An atant ($pendingCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeonPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanNeonPrimary
                    )
                )
                FilterChip(
                    selected = filterCompleted == true,
                    onClick = { filterCompleted = true },
                    label = { Text("Fini ($completedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF10B981)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task List
            if (filteredTasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceContainerHigh.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks found in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Tap 'New Task' to schedule a reminder with date & time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTasks.forEach { task ->
                        TaskItemRow(
                            task = task,
                            onEdit = { taskBeingEdited = task },
                            onToggleCompleted = { repository.toggleTaskCompleted(task.id) },
                            onDelete = {
                                repository.deleteTask(task.id)
                                Toast.makeText(context, "Tach retire!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            initialDraft = reminderDraft,
            onDismiss = {
                showCreateDialog = false
                repository.clearReminderDraft()
            },
            onSave = { newTask ->
                val reminderScheduled = repository.addTask(newTask)
                showCreateDialog = false
                repository.clearReminderDraft()
                Toast.makeText(
                    context,
                    if (reminderScheduled) "Tach anrejistre; rapèl la pwograme."
                    else "Tach la anrejistre, men Android pa t aksepte rapèl la.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
    taskBeingEdited?.let { task ->
        CreateTaskDialog(
            initialTask = task,
            onDismiss = { taskBeingEdited = null },
            onSave = { updatedTask ->
                val reminderScheduled = repository.addTask(updatedTask)
                taskBeingEdited = null
                Toast.makeText(
                    context,
                    if (reminderScheduled) "Tach la modifye; rapèl la pwograme."
                    else "Tach la modifye, men Android pa t aksepte rapèl la.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@Composable
private fun TaskItemRow(
    task: HirisTask,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE dd MMM • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(task.dueDateMillis) {
        dateFormat.format(Date(task.dueDateMillis))
    }
    val isOverdue = !task.isCompleted && task.dueDateMillis < System.currentTimeMillis()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) Color(0xFF10B981).copy(alpha = 0.4f)
            else if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.5f)
            else CyanNeonPrimary.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Checkbox
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (task.isCompleted) "Completed" else "Mark Complete",
                    tint = if (task.isCompleted) Color(0xFF10B981) else CyanNeonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) TextTertiary else TextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = if (isOverdue) Color(0xFFEF4444) else CyanNeonPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isOverdue) "Pase: $formattedDate" else formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) Color(0xFFEF4444) else CyanNeonPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            // Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CreateTaskDialog(
    initialDraft: TaskReminderDraft? = null,
    initialTask: HirisTask? = null,
    onDismiss: () -> Unit,
    onSave: (HirisTask) -> Unit
) {
    val context = LocalContext.current
    var title by remember(initialDraft, initialTask) { mutableStateOf(initialTask?.title ?: initialDraft?.title.orEmpty()) }
    var description by remember(initialDraft, initialTask) { mutableStateOf(initialTask?.description ?: initialDraft?.description.orEmpty()) }
    var priority by remember(initialTask) { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var selectedRingtoneUri by remember(initialTask) { mutableStateOf(initialTask?.ringtoneUri) }
    var selectedRingtoneLabel by remember(initialTask) {
        mutableStateOf(
            initialTask?.ringtoneUri?.let { savedUri ->
                runCatching { RingtoneManager.getRingtone(context, Uri.parse(savedUri))?.getTitle(context) }
                    .getOrNull()?.takeIf { it.isNotBlank() }
            } ?: "Son alam default telefòn nan"
        )
    }
    var pendingTask by remember { mutableStateOf<HirisTask?>(null) }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        pendingTask?.let { task ->
            pendingTask = null
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() != true) {
                Toast.makeText(context, "Alam presi poko aktive; HIRIS ap voye rapèl la nan lè ki pi pre sistèm nan pèmèt.", Toast.LENGTH_LONG).show()
            }
            onSave(task)
        }
    }
    fun saveOrRequestExactAlarm(task: HirisTask) {
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() != true) {
            pendingTask = task
            val settingsIntent = android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching { exactAlarmSettingsLauncher.launch(settingsIntent) }
                .onFailure {
                    pendingTask = null
                    onSave(task)
                    Toast.makeText(context, "Rapèl la ap itilize lè apwoksimatif; pèmèt alam presi nan paramèt pou plis presizyon.", Toast.LENGTH_LONG).show()
                }
        } else {
            onSave(task)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val task = pendingTask
        pendingTask = null
        if (task != null) {
            if (!granted) Toast.makeText(context, "Aktive notifikasyon HIRIS yo nan paramèt telefòn nan pou w resevwa rapèl yo.", Toast.LENGTH_LONG).show()
            saveOrRequestExactAlarm(task)
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri?.toString()
            selectedRingtoneLabel = if (uri == null) {
                "Son alam default telefòn nan"
            } else {
                runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
                    .getOrNull()?.takeIf { it.isNotBlank() } ?: "Son telefòn chwazi"
            }
        }
    }

    val calendar = remember(initialTask) {
        Calendar.getInstance().apply {
            if (initialTask != null) {
                timeInMillis = initialTask.dueDateMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var selectedMillis by remember(initialTask) { mutableStateOf(calendar.timeInMillis) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val use24HourClock = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = remember(use24HourClock) {
        SimpleDateFormat(if (use24HourClock) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    var displayDate by remember(initialTask) { mutableStateOf(dateFormat.format(Date(selectedMillis))) }
    var displayTime by remember(initialTask, use24HourClock) { mutableStateOf(timeFormat.format(Date(selectedMillis))) }

    fun beginSave(task: HirisTask) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingTask = task
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            saveOrRequestExactAlarm(task)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, CyanNeonPrimary),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (initialTask == null) "New Task & Reminder" else "Modify Task & Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeonPrimary
                )
                Text(
                    text = "Set title, date, and time for HIRIS to fire an alarm notification.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E3A70), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Text("🗓  ${SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(selectedMillis))}", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("🔔  Rapèl ap parèt nan notifikasyon lè a rive", color = Color(0xFFBFDBFE), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tit Tach lan *") },
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mesaj rapèl la / Nòt (opsyonèl)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeonPrimary,
                        unfocusedBorderColor = TextTertiary,
                        focusedLabelColor = CyanNeonPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("SON NOTIFIKASYON AN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CyanNeonPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainerHigh,
                    border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        val pickerIntent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chwazi son pou rapèl la")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                        }
                        runCatching { ringtonePickerLauncher.launch(pickerIntent) }
                            .onFailure { Toast.makeText(context, "Mwen pa ka ouvri chwa son telefòn nan.", Toast.LENGTH_SHORT).show() }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = CyanNeonPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedRingtoneLabel, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dat ak Lè Rapèl la:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("+15 min" to 15, "+1 èdtan" to 60, "+3 èdtan" to 180).forEach { (label, minutes) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val quickTime = Calendar.getInstance().apply {
                                    add(Calendar.MINUTE, minutes)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedMillis = quickTime.timeInMillis
                                displayDate = dateFormat.format(Date(selectedMillis))
                                displayTime = timeFormat.format(Date(selectedMillis))
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Date and Time Pickers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Date Picker Trigger
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        selectedMillis = cal.timeInMillis
                                        displayDate = dateFormat.format(Date(selectedMillis))
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Date",
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }

                    // Time Picker Trigger
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        cal.set(Calendar.MINUTE, minute)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        selectedMillis = cal.timeInMillis
                                        displayTime = timeFormat.format(Date(selectedMillis))
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    use24HourClock
                                ).show()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = "Time",
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter a title for the task!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedMillis <= System.currentTimeMillis() && initialTask?.isCompleted != true) {
                                Toast.makeText(context, "Chwazi yon dat ak lè ki poko pase.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val task = HirisTask(
                                id = initialTask?.id?.takeUnless { it.startsWith("sample_") }
                                    ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                dueDateMillis = selectedMillis,
                                priority = priority,
                                ringtoneUri = selectedRingtoneUri,
                                isCompleted = initialTask?.isCompleted ?: false,
                                createdAtMillis = initialTask?.createdAtMillis ?: System.currentTimeMillis()
                            )
                            beginSave(task)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeonPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (initialTask == null) "Save & Schedule" else "Save Changes",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
