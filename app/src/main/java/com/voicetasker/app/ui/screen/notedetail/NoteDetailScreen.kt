package com.voicetasker.app.ui.screen.notedetail

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
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
    val df = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ITALIAN)
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }

    val note = uiState.note
    if (note == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (uiState.isEditing) "Modifica" else "Dettaglio") },
            navigationIcon = { IconButton(onClick = { if (uiState.isEditing) viewModel.cancelEditing() else onNavigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
            actions = { if (uiState.isEditing) { IconButton(onClick = viewModel::saveEdits) { Icon(Icons.Filled.Check, "Salva", tint = MaterialTheme.colorScheme.primary) } } else { IconButton(onClick = viewModel::startEditing) { Icon(Icons.Filled.Edit, "Modifica") }; IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error) } } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (uiState.isEditing) {
                OutlinedTextField(uiState.editTitle, viewModel::onEditTitleChanged, Modifier.fillMaxWidth(), label = { Text("Titolo") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(uiState.editTranscription, viewModel::onEditTranscriptionChanged, Modifier.fillMaxWidth().height(150.dp), label = { Text("Trascrizione") }, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(16.dp))
                Text("Categoria", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.categories.forEach { cat ->
                        val c = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        val sel = uiState.editCategoryId == cat.id
                        Surface(onClick = { viewModel.onEditCategoryChanged(cat.id) }, shape = MaterialTheme.shapes.small, color = if (sel) c.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp)); Text(cat.name, style = MaterialTheme.typography.labelMedium, color = if (sel) c else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Text(note.title.ifBlank { "Nota vocale" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val catColor = uiState.categories.find { it.id == note.categoryId }?.colorHex ?: "#6C63FF"
                val catName = uiState.categories.find { it.id == note.categoryId }?.name ?: ""
                val c = try { Color(android.graphics.Color.parseColor(catColor)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) { Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp)); Text(catName, style = MaterialTheme.typography.labelMedium) } }
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.medium) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(df.format(Date(note.scheduledDate))) } }
                Spacer(Modifier.height(12.dp))
                val min = (note.durationMs / 60000).toInt(); val sec = ((note.durationMs % 60000) / 1000).toInt()
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.medium) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text("Durata: ${String.format("%02d:%02d", min, sec)}") } }
                Spacer(Modifier.height(16.dp))
                Text("Trascrizione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), shape = MaterialTheme.shapes.medium) { Text(note.transcription.ifBlank { "Nessuna trascrizione" }, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
                Spacer(Modifier.height(16.dp))
                Text("Promemoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp))
                if (uiState.reminders.isEmpty()) Text("Nessun promemoria", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else uiState.reminders.forEach { rem -> Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.small) { Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Notifications, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(8.dp)); Text(rem.type.label) }; if (!rem.isTriggered) IconButton(onClick = { viewModel.removeReminder(rem.id) }, Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Rimuovi", Modifier.size(16.dp)) } else Text("✓", color = MaterialTheme.colorScheme.tertiary) } } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ReminderType.entries.filter { type -> uiState.reminders.none { it.type == type } }.forEach { type -> AssistChip(onClick = { viewModel.addReminder(type) }, label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(14.dp)) }) } }
            }
        }
    }
    if (showDeleteDialog) AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Elimina nota") }, text = { Text("Sei sicuro?") }, confirmButton = { TextButton(onClick = { showDeleteDialog = false; viewModel.deleteNote() }) { Text("Elimina", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") } })
}
