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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(onNavigateBack: () -> Unit, viewModel: RecordViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted; if (granted) viewModel.startRecording() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN)

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }

    fun onRecordClick() {
        if (uiState.isRecording) { viewModel.stopRecording() }
        else if (hasPermission) { viewModel.startRecording() }
        else { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registra nota") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
                actions = { if (!uiState.isRecording && uiState.transcription.isNotBlank()) { IconButton(onClick = viewModel::saveNote) { Icon(Icons.Filled.Check, "Salva", tint = MaterialTheme.colorScheme.primary) } } },
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
            Spacer(Modifier.height(24.dp))

            // Record button
            FloatingActionButton(onClick = ::onRecordClick,
                containerColor = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) {
                Icon(if (uiState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic, "Registra", Modifier.size(28.dp), tint = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(if (uiState.isRecording) "Tocca per fermare" else if (uiState.transcription.isNotBlank()) "Completata" else "Tocca per registrare", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Error message
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                            Text("✨ Gemini sta analizzando la nota...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Title
                OutlinedTextField(uiState.title, viewModel::onTitleChanged, Modifier.fillMaxWidth(),
                    label = { Text(if (uiState.aiTitleSuggestion != null) "Titolo (suggerito da AI ✨)" else "Titolo") },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Title, null) })
                Spacer(Modifier.height(12.dp))

                // Transcription
                OutlinedTextField(uiState.transcription, viewModel::onTranscriptionChanged,
                    Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text("Descrizione") }, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Notes, null) })
                Spacer(Modifier.height(12.dp))

                // Date
                OutlinedButton(onClick = { showDatePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.CalendarMonth, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.noteDate.isNotBlank()) "📅 ${uiState.noteDate}" else "📅 ${df.format(Date(uiState.scheduledDate))}")
                }
                Spacer(Modifier.height(12.dp))

                // Time
                OutlinedButton(onClick = { showTimePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.AccessTime, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.noteTime.isNotBlank()) "🕐 ${uiState.noteTime}" + if (uiState.aiTitleSuggestion != null) " (AI ✨)" else "" else "🕐 Imposta ora")
                }
                Spacer(Modifier.height(12.dp))

                // Location
                OutlinedTextField(uiState.location, viewModel::onLocationChanged, Modifier.fillMaxWidth(),
                    label = { Text(if (uiState.location.isNotBlank()) "Dove (estratto da AI ✨)" else "Dove") },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                    placeholder = { Text("es. Ufficio, Roma...") })
                Spacer(Modifier.height(16.dp))

                // Category
                Text("Categoria" + if (uiState.selectedCategoryId != null && uiState.aiTitleSuggestion != null) " (suggerita da AI ✨)" else "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    Text("Salva nota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Date picker
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = uiState.scheduledDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { viewModel.onScheduledDateChanged(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla") } }
        ) { DatePicker(state = dps) }
    }

    // Time picker
    if (showTimePicker) {
        val initialHour = uiState.noteTime.split(":").getOrNull(0)?.toIntOrNull() ?: 12
        val initialMinute = uiState.noteTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        val tps = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false },
            title = { Text("Seleziona ora") },
            text = { TimePicker(state = tps) },
            confirmButton = { TextButton(onClick = { viewModel.onTimeChanged(String.format("%02d:%02d", tps.hour, tps.minute)); showTimePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Annulla") } }
        )
    }
}
