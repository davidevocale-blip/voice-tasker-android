package com.voicetasker.app.ui.screen.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.ui.localization.localizedDateFormatter
import com.voicetasker.app.ui.localization.resourceLocale
import com.voicetasker.app.ui.resources.asString
import com.voicetasker.app.ui.resources.displayName
import com.voicetasker.app.ui.theme.VoiceTaskerSizing
import com.voicetasker.app.ui.theme.VoiceTaskerSpacing
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.startRecording()
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val locale = resourceLocale()
    val dateFormatter = remember(locale) {
        localizedDateFormatter(locale)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }
    LaunchedEffect(uiState.recordingDurationMs, uiState.isRecording) {
        if (uiState.isRecording && uiState.recordingDurationMs >= uiState.maxDurationMs) {
            viewModel.stopRecording()
        }
    }

    fun onRecordClick() {
        if (uiState.isRecording) {
            viewModel.stopRecording()
        } else if (hasPermission) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.record_note),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!uiState.isRecording && uiState.transcription.isNotBlank()) {
                        IconButton(
                            onClick = viewModel::saveNote,
                            modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.save),
                                tint = MaterialTheme.colorScheme.primary
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
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecordingPanel(
                isRecording = uiState.isRecording,
                durationMs = uiState.recordingDurationMs,
                maxDurationMs = uiState.maxDurationMs,
                isPremium = uiState.isPremium,
                amplitudes = uiState.amplitudes,
                hasTranscription = uiState.transcription.isNotBlank(),
                onRecordClick = ::onRecordClick
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(VoiceTaskerSpacing.md),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                            Text(
                                text = message.asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (uiState.authenticationRequired) {
                            TextButton(
                                onClick = onNavigateToLogin,
                                modifier = Modifier.heightIn(
                                    min = VoiceTaskerSizing.minimumTouchTarget
                                )
                            ) {
                                Text(stringResource(R.string.sign_in))
                            }
                        }
                    }
                }
            }

            if (!uiState.isRecording && uiState.transcription.isNotBlank()) {
                Spacer(Modifier.height(VoiceTaskerSpacing.lg))

                if (uiState.isAiProcessing) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(VoiceTaskerSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(VoiceTaskerSpacing.sm))
                            Text(
                                text = stringResource(R.string.gemini_analyzing_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = viewModel::requestAiProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                        Text(stringResource(R.string.process_with_ai))
                    }
                }

                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (uiState.aiTitleSuggestion != null) {
                                    R.string.note_title_ai_suggested
                                } else {
                                    R.string.note_title
                                }
                            )
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Title, contentDescription = null) }
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedTextField(
                    value = uiState.transcription,
                    onValueChange = viewModel::onTranscriptionChanged,
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
                            uiState.noteDate.ifBlank {
                                dateFormatter.format(Date(uiState.scheduledDate))
                            }
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
                        if (uiState.noteTime.isNotBlank()) {
                            stringResource(
                                if (uiState.aiTitleSuggestion != null) {
                                    R.string.time_value_ai
                                } else {
                                    R.string.time_value
                                },
                                uiState.noteTime
                            )
                        } else {
                            stringResource(R.string.set_time)
                        }
                    )
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.sm))
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = viewModel::onLocationChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (uiState.location.isNotBlank()) {
                                    R.string.location_ai_extracted
                                } else {
                                    R.string.location
                                }
                            )
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                    },
                    placeholder = { Text(stringResource(R.string.location_example)) }
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.md))
                Text(
                    text = stringResource(
                        if (
                            uiState.selectedCategoryId != null &&
                            uiState.aiTitleSuggestion != null
                        ) {
                            R.string.category_ai_suggested
                        } else {
                            R.string.category
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs)
                ) {
                    uiState.categories.forEach { category ->
                        val categoryColor = categoryColorOrPrimary(category.colorHex)
                        val selected = uiState.selectedCategoryId == category.id
                        Surface(
                            onClick = { viewModel.onCategorySelected(category.id) },
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
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor)
                                )
                                Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                                Text(
                                    text = category.displayName().asString(),
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
                    onClick = viewModel::saveNote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                    enabled = uiState.title.isNotBlank() ||
                        uiState.transcription.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                    Text(
                        text = stringResource(R.string.save_note),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(VoiceTaskerSpacing.xxl))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.scheduledDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(
                        viewModel::onScheduledDateChanged
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
        val initialHour = uiState.noteTime
            .split(":")
            .getOrNull(0)
            ?.toIntOrNull() ?: 12
        val initialMinute = uiState.noteTime
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
                    viewModel.onTimeChanged(
                        String.format(
                            Locale.ROOT,
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
private fun RecordingPanel(
    isRecording: Boolean,
    durationMs: Long,
    maxDurationMs: Long,
    isPremium: Boolean,
    amplitudes: List<Float>,
    hasTranscription: Boolean,
    onRecordClick: () -> Unit
) {
    val minutes = (durationMs / 60_000).toInt()
    val seconds = ((durationMs % 60_000) / 1_000).toInt()
    val maxMinutes = (maxDurationMs / 60_000).toInt()
    val nearLimit = durationMs >= maxDurationMs * 0.8
    val accentColor = if (isRecording) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = VoiceTaskerSpacing.md,
                vertical = VoiceTaskerSpacing.lg
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = if (isRecording) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = VoiceTaskerSpacing.sm,
                        vertical = VoiceTaskerSpacing.xs
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(VoiceTaskerSpacing.xs))
                    Text(
                        text = stringResource(
                            if (isRecording) {
                                R.string.tap_to_stop
                            } else if (hasTranscription) {
                                R.string.recording_completed
                            } else {
                                R.string.tap_to_record
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRecording) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(Modifier.height(VoiceTaskerSpacing.md))
            Waveform(
                amplitudes = amplitudes,
                isRecording = isRecording,
                color = accentColor
            )
            Spacer(Modifier.height(VoiceTaskerSpacing.sm))
            Text(
                text = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    if (isPremium) {
                        R.string.max_recording_minutes
                    } else {
                        R.string.max_recording_minutes_free
                    },
                    maxMinutes
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (nearLimit) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(VoiceTaskerSpacing.md))
            FloatingActionButton(
                onClick = onRecordClick,
                modifier = Modifier.size(64.dp),
                containerColor = accentColor,
                contentColor = if (isRecording) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
            ) {
                Icon(
                    imageVector = if (isRecording) {
                        Icons.Filled.Stop
                    } else {
                        Icons.Filled.Mic
                    },
                    contentDescription = stringResource(R.string.record),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun Waveform(
    amplitudes: List<Float>,
    isRecording: Boolean,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        val barCount = 36
        val barWidth = (size.width / barCount) * 0.55f
        val spacing = (size.width / barCount) * 0.45f
        val visibleAmplitudes = amplitudes.takeLast(barCount)
        for (index in 0 until barCount) {
            val amplitude = if (index < visibleAmplitudes.size && isRecording) {
                (visibleAmplitudes[index] / 10f).coerceIn(0.05f, 1f)
            } else {
                0.1f
            }
            val barHeight = (amplitude * size.height).coerceIn(4f, size.height)
            drawRoundRect(
                color = color.copy(alpha = 0.28f + amplitude * 0.72f),
                topLeft = Offset(
                    x = index * (barWidth + spacing) + spacing / 2,
                    y = (size.height - barHeight) / 2
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

@Composable
private fun categoryColorOrPrimary(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
}
