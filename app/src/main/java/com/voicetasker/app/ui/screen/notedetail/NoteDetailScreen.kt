package com.voicetasker.app.ui.screen.notedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.ui.localization.localizedDateFormatter
import com.voicetasker.app.ui.localization.resourceLocale
import com.voicetasker.app.ui.resources.labelRes
import com.voicetasker.app.ui.theme.VoiceTaskerDesign
import com.voicetasker.app.ui.theme.VoiceTaskerSizing
import com.voicetasker.app.ui.theme.VoiceTaskerSpacing
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: (String) -> Unit = {},
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val locale = resourceLocale()
    val dateFormatter = remember(locale) {
        localizedDateFormatter(locale)
    }
    val context = LocalContext.current

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    val note = uiState.note
    if (note == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditing) R.string.edit else R.string.note_detail
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isEditing) {
                                viewModel.cancelEditing()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(
                            onClick = viewModel::saveEdits,
                            modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.save),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = viewModel::startEditing,
                            modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = VoiceTaskerSpacing.md,
                    vertical = VoiceTaskerSpacing.sm
                )
        ) {
            if (uiState.isEditing) {
                OutlinedTextField(
                    value = uiState.editTitle,
                    onValueChange = viewModel::onEditTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.note_title)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Title, contentDescription = null) }
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedTextField(
                    value = uiState.editTranscription,
                    onValueChange = viewModel::onEditTranscriptionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    label = { Text(stringResource(R.string.description)) },
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null) }
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                    Text(
                        stringResource(
                            R.string.date_value,
                            dateFormatter.format(Date(uiState.editScheduledDate))
                        )
                    )
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null)
                    Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                    Text(
                        if (uiState.editNoteTime.isNotBlank()) {
                            stringResource(R.string.time_value, uiState.editNoteTime)
                        } else {
                            stringResource(R.string.set_time)
                        }
                    )
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedTextField(
                    value = uiState.editLocation,
                    onValueChange = viewModel::onEditLocationChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.location)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                    },
                    placeholder = { Text(stringResource(R.string.location_example)) }
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.md))
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs)
                ) {
                    uiState.categories.forEach { category ->
                        val color = parseCategoryColor(category.colorHex)
                        val selected = uiState.editCategoryId == category.id
                        Surface(
                            onClick = { viewModel.onEditCategoryChanged(category.id) },
                            modifier = Modifier.heightIn(
                                min = VoiceTaskerSizing.minimumTouchTarget
                            ),
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = VoiceTaskerSpacing.sm,
                                    vertical = VoiceTaskerSpacing.xs
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryDot(color)
                                Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.xl))
                Button(
                    onClick = viewModel::saveEdits,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                    Text(stringResource(R.string.save_changes))
                }
            } else {
                val categoryColor = parseCategoryColor(
                    viewModel.getCategoryColor(note.categoryId)
                )
                val categoryName = viewModel.getCategoryName(note.categoryId)

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = VoiceTaskerSpacing.sm,
                            vertical = VoiceTaskerSpacing.xs
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryDot(categoryColor)
                        Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                Text(
                    text = note.title.ifBlank { stringResource(R.string.voice_note) },
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.lg))

                Text(
                    text = stringResource(R.string.description),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                SectionCard {
                    Text(
                        text = note.transcription.ifBlank {
                            stringResource(R.string.no_description)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.md))

                SectionCard(contentPadding = 0.dp) {
                    InfoRow(
                        icon = Icons.Filled.CalendarMonth,
                        label = stringResource(R.string.data_label),
                        value = dateFormatter.format(Date(note.scheduledDate))
                    )
                    if (note.noteTime.isNotBlank()) {
                        InfoDivider()
                        InfoRow(
                            icon = Icons.Filled.AccessTime,
                            label = stringResource(R.string.time_label),
                            value = note.noteTime
                        )
                    }
                    if (note.location.isNotBlank()) {
                        InfoDivider()
                        InfoRow(
                            icon = Icons.Filled.LocationOn,
                            label = stringResource(R.string.location),
                            value = note.location,
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = {
                                val uri = Uri.parse(
                                    "https://www.google.com/maps/search/?api=1&query=" +
                                        Uri.encode(note.location)
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier
                                .padding(
                                    start = VoiceTaskerSpacing.sm,
                                    end = VoiceTaskerSpacing.sm,
                                    bottom = VoiceTaskerSpacing.xs
                                )
                                .heightIn(min = VoiceTaskerSizing.minimumTouchTarget)
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null)
                            Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                            Text(
                                text = stringResource(R.string.open_in_google_maps),
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                    if (note.durationMs > 0) {
                        val minutes = (note.durationMs / 60_000).toInt()
                        val seconds = ((note.durationMs % 60_000) / 1_000).toInt()
                        InfoDivider()
                        InfoRow(
                            icon = Icons.Filled.Timer,
                            label = stringResource(R.string.recording_duration),
                            value = String.format("%02d:%02d", minutes, seconds)
                        )
                    }
                }

                Spacer(Modifier.height(VoiceTaskerSpacing.lg))
                Text(
                    text = stringResource(R.string.reminders),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                SectionCard(contentPadding = VoiceTaskerSpacing.sm) {
                    if (uiState.reminders.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_reminders),
                            modifier = Modifier.padding(VoiceTaskerSpacing.xxs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.reminders.forEachIndexed { index, reminder ->
                            if (index > 0) InfoDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        min = VoiceTaskerSizing.minimumTouchTarget
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                                Text(
                                    text = stringResource(reminder.type.labelRes()),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!reminder.isTriggered) {
                                    IconButton(
                                        onClick = {
                                            viewModel.removeReminder(reminder.id)
                                        },
                                        modifier = Modifier.size(
                                            VoiceTaskerSizing.minimumTouchTarget
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(
                                                R.string.remove
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.completed_mark),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                if (uiState.isPremium) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            VoiceTaskerSpacing.xs
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            VoiceTaskerSpacing.xs
                        )
                    ) {
                        ReminderType.entries
                            .filter { type ->
                                uiState.reminders.none { it.type == type }
                            }
                            .forEach { type ->
                                AssistChip(
                                    onClick = { viewModel.addReminder(type) },
                                    modifier = Modifier.heightIn(
                                        min = VoiceTaskerSizing.minimumTouchTarget
                                    ),
                                    label = {
                                        Text(
                                            text = stringResource(type.labelRes()),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                    }
                } else {
                    Surface(
                        onClick = { onNavigateToPaywall("reminder") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
                        shape = MaterialTheme.shapes.medium,
                        color = VoiceTaskerDesign.colors.premiumContainer,
                        border = BorderStroke(
                            1.dp,
                            VoiceTaskerDesign.colors.premiumGold.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(VoiceTaskerSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                            Text(
                                text = stringResource(
                                    R.string.unlock_reminders_premium
                                ),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = VoiceTaskerDesign.colors.premiumGold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.xxl))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_note)) },
            text = { Text(stringResource(R.string.delete_note_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteNote()
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.editScheduledDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(
                        viewModel::onEditDateChanged
                    )
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialHour = uiState.editNoteTime
            .split(":")
            .getOrNull(0)
            ?.toIntOrNull() ?: 12
        val initialMinute = uiState.editNoteTime
            .split(":")
            .getOrNull(1)
            ?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.select_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEditTimeChanged(
                        String.format(
                            "%02d:%02d",
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    contentPadding: androidx.compose.ui.unit.Dp = VoiceTaskerSpacing.md,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(
                horizontal = VoiceTaskerSpacing.md,
                vertical = VoiceTaskerSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }
        }
        Spacer(Modifier.width(VoiceTaskerSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun InfoDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun CategoryDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun parseCategoryColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
}
