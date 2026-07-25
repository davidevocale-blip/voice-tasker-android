package com.voicetasker.app.ui.screen.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.ui.resources.asString
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted; if (granted) viewModel.startRecording() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN)

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }

    // Auto-stop recording when hitting the duration limit
    LaunchedEffect(uiState.recordingDurationMs, uiState.isRecording) {
        if (uiState.isRecording && uiState.recordingDurationMs >= uiState.maxDurationMs) {
            viewModel.stopRecording()
        }
    }

    fun onRecordClick() {
        if (uiState.isRecording) { viewModel.stopRecording() }
        else if (hasPermission) { viewModel.startRecording() }
        else { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.record_note)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = { if (!uiState.isRecording && uiState.transcription.isNotBlank()) { IconButton(onClick = viewModel::saveNote) { Icon(Icons.Filled.Check, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary) } } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }, containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            // Waveform
            val barColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val barCount = 40; val barW = (size.width / barCount) * 0.6f; val sp = (size.width / barCount) * 0.4f
                val amps = uiState.amplitudes.takeLast(barCount)
                for (i in 0 until barCount) {
                    val amp = if (i < amps.size && uiState.isRecording) (amps[i] / 10f).coerceIn(0.05f, 1f) else 0.1f
                    val h = (amp * size.height).coerceIn(4f, size.height)
                    drawRoundRect(barColor.copy(0.3f + amp * 0.7f), Offset(i * (barW + sp) + sp / 2, (size.height - h) / 2), Size(barW, h), CornerRadius(barW / 2))
                }
            }
            Spacer(Modifier.height(16.dp))
            val min = (uiState.recordingDurationMs / 60000).toInt(); val sec = ((uiState.recordingDurationMs % 60000) / 1000).toInt()
            Text(String.format("%02d:%02d", min, sec), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light)
            // Max duration label
            val maxMin = (uiState.maxDurationMs / 60000).toInt()
            Text(
                stringResource(if (uiState.isPremium) R.string.max_recording_minutes else R.string.max_recording_minutes_free, maxMin),
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.recordingDurationMs >= uiState.maxDurationMs * 0.8) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            // Record button
            FloatingActionButton(onClick = ::onRecordClick,
                containerColor = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) {
                Icon(if (uiState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic, stringResource(R.string.record), Modifier.size(28.dp), tint = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(if (uiState.isRecording) R.string.tap_to_stop else if (uiState.transcription.isNotBlank()) R.string.recording_completed else R.string.tap_to_record), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Error message
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (uiState.authenticationRequired) {
                    TextButton(onClick = onNavigateToLogin) {
                        Text(stringResource(R.string.sign_in))
                    }
                }
            }

            // --- Form after recording ---
            if (!uiState.isRecording && uiState.transcription.isNotBlank()) {
                Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

                // AI processing indicator
                if (uiState.isAiProcessing) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer.copy(0.5f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.gemini_analyzing_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    OutlinedButton(
                        onClick = viewModel::requestAiProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.process_with_ai))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Title
                OutlinedTextField(uiState.title, viewModel::onTitleChanged, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(if (uiState.aiTitleSuggestion != null) R.string.note_title_ai_suggested else R.string.note_title)) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Title, null) })
                Spacer(Modifier.height(12.dp))

                // Transcription
                OutlinedTextField(uiState.transcription, viewModel::onTranscriptionChanged,
                    Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text(stringResource(R.string.description)) }, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Notes, null) })
                Spacer(Modifier.height(12.dp))

                // Date
                OutlinedButton(onClick = { showDatePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.CalendarMonth, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.date_value, if (uiState.noteDate.isNotBlank()) uiState.noteDate else df.format(Date(uiState.scheduledDate))))
                }
                Spacer(Modifier.height(12.dp))

                // Time
                OutlinedButton(onClick = { showTimePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.AccessTime, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.noteTime.isNotBlank()) stringResource(if (uiState.aiTitleSuggestion != null) R.string.time_value_ai else R.string.time_value, uiState.noteTime) else stringResource(R.string.set_time))
                }
                Spacer(Modifier.height(12.dp))

                // Location
                OutlinedTextField(uiState.location, viewModel::onLocationChanged, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(if (uiState.location.isNotBlank()) R.string.location_ai_extracted else R.string.location)) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                    placeholder = { Text(stringResource(R.string.location_example)) })
                Spacer(Modifier.height(16.dp))

                // Category
                Text(stringResource(if (uiState.selectedCategoryId != null && uiState.aiTitleSuggestion != null) R.string.category_ai_suggested else R.string.category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    uiState.categories.forEach { cat ->
                        val c = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        val sel = uiState.selectedCategoryId == cat.id
                        Surface(onClick = { viewModel.onCategorySelected(cat.id) }, shape = MaterialTheme.shapes.small,
                            color = if (sel) c.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                                Spacer(Modifier.width(6.dp))
                                Text(cat.name, style = MaterialTheme.typography.labelMedium, color = if (sel) c else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                // Save button
                Button(onClick = viewModel::saveNote, Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium,
                    enabled = uiState.title.isNotBlank() || uiState.transcription.isNotBlank()) {
                    Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Date picker
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = uiState.scheduledDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { viewModel.onScheduledDateChanged(it) }; showDatePicker = false }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = dps) }
    }

    // Time picker
    if (showTimePicker) {
        val initialHour = uiState.noteTime.split(":").getOrNull(0)?.toIntOrNull() ?: 12
        val initialMinute = uiState.noteTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        val tps = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.select_time)) },
            text = { TimePicker(state = tps) },
            confirmButton = { TextButton(onClick = { viewModel.onTimeChanged(String.format("%02d:%02d", tps.hour, tps.minute)); showTimePicker = false }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
