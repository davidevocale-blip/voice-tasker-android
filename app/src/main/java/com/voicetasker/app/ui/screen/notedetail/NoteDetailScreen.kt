package com.voicetasker.app.ui.screen.notedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.domain.model.ReminderType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(onNavigateBack: () -> Unit, viewModel: NoteDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN)
    val context = LocalContext.current
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }

    val note = uiState.note
    if (note == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (uiState.isEditing) "Modifica" else "Dettaglio nota") },
            navigationIcon = { IconButton(onClick = { if (uiState.isEditing) viewModel.cancelEditing() else onNavigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
            actions = {
                if (uiState.isEditing) {
                    IconButton(onClick = viewModel::saveEdits) { Icon(Icons.Filled.Check, "Salva", tint = MaterialTheme.colorScheme.primary) }
                } else {
                    IconButton(onClick = viewModel::startEditing) { Icon(Icons.Filled.Edit, "Modifica") }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error) }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (uiState.isEditing) {
                // ── EDIT MODE ──
                OutlinedTextField(uiState.editTitle, viewModel::onEditTitleChanged, Modifier.fillMaxWidth(),
                    label = { Text("Titolo") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Title, null) })
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(uiState.editTranscription, viewModel::onEditTranscriptionChanged,
                    Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    label = { Text("Descrizione") }, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.Notes, null) })
                Spacer(Modifier.height(12.dp))

                // Date
                OutlinedButton(onClick = { showDatePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(8.dp))
                    Text("📅 ${df.format(Date(uiState.editScheduledDate))}")
                }
                Spacer(Modifier.height(12.dp))

                // Time
                OutlinedButton(onClick = { showTimePicker = true }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.AccessTime, null); Spacer(Modifier.width(8.dp))
                    Text(if (uiState.editNoteTime.isNotBlank()) "🕐 ${uiState.editNoteTime}" else "🕐 Imposta ora")
                }
                Spacer(Modifier.height(12.dp))

                // Location
                OutlinedTextField(uiState.editLocation, viewModel::onEditLocationChanged, Modifier.fillMaxWidth(),
                    label = { Text("Dove") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                    placeholder = { Text("es. Ufficio, Roma...") })
                Spacer(Modifier.height(16.dp))

                // Category
                Text("Categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.categories.forEach { cat ->
                        val c = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        val sel = uiState.editCategoryId == cat.id
                        Surface(onClick = { viewModel.onEditCategoryChanged(cat.id) }, shape = MaterialTheme.shapes.small,
                            color = if (sel) c.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp))
                                Text(cat.name, style = MaterialTheme.typography.labelMedium, color = if (sel) c else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(onClick = viewModel::saveEdits, Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp)); Text("Salva modifiche")
                }
            } else {
                // ── VIEW MODE ──
                // Category chip
                val catColor = viewModel.getCategoryColor(note.categoryId)
                val catName = viewModel.getCategoryName(note.categoryId)
                val c = try { Color(android.graphics.Color.parseColor(catColor)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                Surface(shape = MaterialTheme.shapes.small, color = c.copy(0.15f)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp))
                        Text(catName, style = MaterialTheme.typography.labelMedium, color = c, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Title
                Text(note.title.ifBlank { "Nota vocale" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // Description
                Text("Descrizione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), shape = MaterialTheme.shapes.medium) {
                    Text(note.transcription.ifBlank { "Nessuna descrizione" }, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(16.dp))

                // Info cards
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                Spacer(Modifier.height(12.dp))

                // Date
                InfoRow(icon = Icons.Filled.CalendarMonth, label = "Data", value = df.format(Date(note.scheduledDate)))

                // Time
                if (note.noteTime.isNotBlank()) {
                    InfoRow(icon = Icons.Filled.AccessTime, label = "Ora", value = note.noteTime)
                }

                // Location with Google Maps link
                if (note.location.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)), shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Dove", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(note.location, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(note.location)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) {
                                Icon(Icons.Filled.Map, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("📍 Apri in Google Maps", textDecoration = TextDecoration.Underline)
                            }
                        }
                    }
                }

                // Duration
                Spacer(Modifier.height(8.dp))
                val min = (note.durationMs / 60000).toInt(); val sec = ((note.durationMs % 60000) / 1000).toInt()
                if (note.durationMs > 0) {
                    InfoRow(icon = Icons.Filled.Timer, label = "Durata registrazione", value = String.format("%02d:%02d", min, sec))
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                Spacer(Modifier.height(12.dp))

                // Reminders
                Text("Promemoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (uiState.reminders.isEmpty()) {
                    Text("Nessun promemoria", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    uiState.reminders.forEach { rem ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.small) {
                            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Notifications, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(8.dp)); Text(rem.type.label) }
                                if (!rem.isTriggered) IconButton(onClick = { viewModel.removeReminder(rem.id) }, Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Rimuovi", Modifier.size(16.dp)) }
                                else Text("✓", color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderType.entries.filter { type -> uiState.reminders.none { it.type == type } }.forEach { type ->
                        AssistChip(onClick = { viewModel.addReminder(type) }, label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(14.dp)) })
                    }
                }
            }
        }
    }

    if (showDeleteDialog) AlertDialog(onDismissRequest = { showDeleteDialog = false },
        title = { Text("Elimina nota") }, text = { Text("Sei sicuro di voler eliminare questa nota?") },
        confirmButton = { TextButton(onClick = { showDeleteDialog = false; viewModel.deleteNote() }) { Text("Elimina", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") } })

    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = uiState.editScheduledDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dps.selectedDateMillis?.let { viewModel.onEditDateChanged(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla") } }
        ) { DatePicker(state = dps) }
    }

    if (showTimePicker) {
        val initialHour = uiState.editNoteTime.split(":").getOrNull(0)?.toIntOrNull() ?: 12
        val initialMinute = uiState.editNoteTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        val tps = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false },
            title = { Text("Seleziona ora") },
            text = { TimePicker(state = tps) },
            confirmButton = { TextButton(onClick = { viewModel.onEditTimeChanged(String.format("%02d:%02d", tps.hour, tps.minute)); showTimePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}
