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
import com.voicetasker.app.domain.model.ReminderType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(onNavigateBack: () -> Unit, viewModel: RecordViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ITALIAN)
    val context = LocalContext.current
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }

    // Runtime permission request
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.startRecording()
    }

    fun onRecordClick() {
        if (uiState.isRecording) {
            viewModel.stopRecording()
        } else {
            if (hasAudioPermission) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registra nota") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
                actions = { if (!uiState.isRecording && uiState.audioFilePath != null) { IconButton(onClick = viewModel::saveNote) { Icon(Icons.Filled.Check, "Salva", tint = MaterialTheme.colorScheme.primary) } } },
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
                    val amp = if (i < amps.size && uiState.isRecording) (amps[i] / 32767f).coerceIn(0.05f, 1f) else 0.1f
                    val h = (amp * size.height).coerceIn(4f, size.height)
                    drawRoundRect(barColor.copy(0.3f + amp * 0.7f), Offset(i * (barW + sp) + sp / 2, (size.height - h) / 2), Size(barW, h), CornerRadius(barW / 2))
                }
            }
            Spacer(Modifier.height(16.dp))
            val min = (uiState.recordingDurationMs / 60000).toInt(); val sec = ((uiState.recordingDurationMs % 60000) / 1000).toInt()
            Text(String.format("%02d:%02d", min, sec), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(24.dp))
            // Record button with permission handling
            FloatingActionButton(onClick = ::onRecordClick,
                containerColor = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) {
                Icon(if (uiState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic, "Registra", Modifier.size(28.dp), tint = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(if (uiState.isRecording) "Tocca per fermare" else if (uiState.audioFilePath != null) "Completata" else "Tocca per registrare", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!uiState.isRecording && uiState.audioFilePath != null) {
                Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
                OutlinedTextField(uiState.title, viewModel::onTitleChanged, Modifier.fillMaxWidth(), label = { Text("Titolo") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(uiState.transcription, viewModel::onTranscriptionChanged, Modifier.fillMaxWidth().height(120.dp), label = { Text("Trascrizione") }, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(16.dp))
                // Categories
                Text("Categoria", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.categories.forEach { cat ->
                        val c = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        val sel = uiState.selectedCategoryId == cat.id
                        Surface(onClick = { viewModel.onCategorySelected(cat.id) }, shape = MaterialTheme.shapes.small, color = if (sel) c.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp)); Text(cat.name, style = MaterialTheme.typography.labelMedium, color = if (sel) c else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Date
                OutlinedButton(onClick = { showDatePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text(df.format(Date(uiState.scheduledDate))) }
                Spacer(Modifier.height(16.dp))
                // Reminders
                Text("Promemoria", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                ReminderType.entries.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Checkbox(type in uiState.selectedReminders, { viewModel.onReminderToggled(type) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                        Text(type.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = viewModel::saveNote, Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) { Text("Salva nota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(32.dp))
            }
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = uiState.scheduledDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { viewModel.onScheduledDateChanged(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla") } }
        ) { DatePicker(state = dps) }
    }
}
