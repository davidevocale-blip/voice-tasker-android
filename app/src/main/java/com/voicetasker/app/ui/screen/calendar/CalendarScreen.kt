package com.voicetasker.app.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onNavigateToNoteDetail: (Long) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mf = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
    val df = SimpleDateFormat("HH:mm", Locale.ITALIAN)
    val today = Calendar.getInstance()
    val selCal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
    val cm = uiState.currentMonth
    val daysInMonth = cm.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = Calendar.getInstance().apply { set(cm.get(Calendar.YEAR), cm.get(Calendar.MONTH), 1) }.get(Calendar.DAY_OF_WEEK)
    val startOffset = (firstDow + 5) % 7
    val dayNames = listOf("L", "M", "M", "G", "V", "S", "D")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.onMonthChanged(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev") }
                            Text(mf.format(cm.time).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { viewModel.onMonthChanged(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
                        }
                        Row(Modifier.fillMaxWidth()) { dayNames.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        Spacer(Modifier.height(4.dp))
                        var day = 1; val rows = (startOffset + daysInMonth + 6) / 7
                        for (r in 0 until rows) {
                            Row(Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val idx = r * 7 + c
                                    if (idx < startOffset || day > daysInMonth) { Box(Modifier.weight(1f).height(42.dp)) }
                                    else {
                                        val d = day
                                        val isToday = d == today.get(Calendar.DAY_OF_MONTH) && cm.get(Calendar.MONTH) == today.get(Calendar.MONTH) && cm.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                                        val isSel = d == selCal.get(Calendar.DAY_OF_MONTH) && cm.get(Calendar.MONTH) == selCal.get(Calendar.MONTH) && cm.get(Calendar.YEAR) == selCal.get(Calendar.YEAR)
                                        val hasNotes = d in uiState.daysWithNotes

                                        Box(Modifier.weight(1f).height(42.dp)
                                            .clip(CircleShape)
                                            .then(if (isSel) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier)
                                            .clickable { val cal = Calendar.getInstance(); cal.set(cm.get(Calendar.YEAR), cm.get(Calendar.MONTH), d, 0, 0, 0); viewModel.onDateSelected(cal.timeInMillis) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(d.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                // Dot indicator for days with notes
                                                if (hasNotes) {
                                                    Box(Modifier.size(5.dp).clip(CircleShape).background(
                                                        if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                    ))
                                                } else {
                                                    Spacer(Modifier.height(5.dp))
                                                }
                                            }
                                        }
                                        day++
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Note del giorno", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
            }
            if (uiState.notesForDate.isEmpty()) {
                item { Text("Nessun impegno", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp)) }
            } else {
                items(uiState.notesForDate, key = { it.id }) { note ->
                    val catColor = viewModel.getCategoryColor(note.categoryId)
                    val catName = viewModel.getCategoryName(note.categoryId)
                    Card(onClick = { onNavigateToNoteDetail(note.id) }, Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.padding(12.dp)) {
                            Box(Modifier.size(4.dp, 48.dp).clip(CircleShape).background(catColor))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(note.title.ifBlank { "Nota vocale" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(note.transcription.take(80), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Category chip
                                    Surface(shape = MaterialTheme.shapes.extraSmall, color = catColor.copy(0.15f)) {
                                        Text(catName, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = catColor)
                                    }
                                    // Time
                                    if (note.noteTime.isNotBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Filled.AccessTime, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(2.dp))
                                        Text(note.noteTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    // Location
                                    if (note.location.isNotBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Filled.LocationOn, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(2.dp))
                                        Text(note.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
