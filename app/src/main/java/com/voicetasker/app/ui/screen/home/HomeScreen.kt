package com.voicetasker.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.ui.theme.Gold40
import com.voicetasker.app.ui.theme.Purple40
import com.voicetasker.app.ui.theme.Purple60
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRecord: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("VoiceTasker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        floatingActionButton = { FloatingActionButton(onClick = onNavigateToRecord, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Mic, "Registra", tint = Color.White) } },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Premium banner
            if (!uiState.isPremium) {
                item {
                    Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Purple40, Purple60)), MaterialTheme.shapes.medium).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = Gold40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Passa a Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${uiState.freeNotesRemaining} note gratuite rimanenti", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                            }
                            Button(onClick = onNavigateToSettings, colors = ButtonDefaults.buttonColors(containerColor = Gold40, contentColor = Purple40), shape = MaterialTheme.shapes.small) { Text("Upgrade", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
            // Search
            item {
                OutlinedTextField(uiState.searchQuery, viewModel::onSearchQueryChanged, Modifier.fillMaxWidth(), placeholder = { Text("Cerca nelle note...") }, leadingIcon = { Icon(Icons.Filled.Search, "Cerca") }, singleLine = true, shape = MaterialTheme.shapes.medium)
            }
            // Category chips
            if (uiState.categories.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.categories) { cat ->
                            val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                            val sel = uiState.selectedCategoryId == cat.id
                            Surface(onClick = { viewModel.onCategoryFilterChanged(cat.id) }, shape = MaterialTheme.shapes.small, color = if (sel) color.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(6.dp))
                                    Text(cat.name, style = MaterialTheme.typography.labelMedium, color = if (sel) color else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            // Notes
            if (uiState.notes.isEmpty() && !uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.MicNone, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("Nessuna nota vocale", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Premi il pulsante per registrare", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    }
                }
            }
            items(uiState.notes, key = { it.id }) { note -> NoteCardItem(note, viewModel.getCategoryColor(note.categoryId), viewModel.getCategoryName(note.categoryId)) { onNavigateToNoteDetail(note.id) } }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun NoteCardItem(note: Note, catColor: Color, catName: String, onClick: () -> Unit) {
    val df = SimpleDateFormat("dd MMM, HH:mm", Locale.ITALIAN)
    Card(onClick = onClick, Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(16.dp)) {
            Box(Modifier.size(4.dp, 48.dp).clip(CircleShape).background(catColor))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(note.title.ifBlank { "Nota vocale" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(note.transcription.ifBlank { "Nessuna trascrizione" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(catColor)); Spacer(Modifier.width(4.dp)); Text(catName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AccessTime, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(df.format(Date(note.scheduledDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
