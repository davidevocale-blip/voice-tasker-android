package com.voicetasker.app.ui.screen.addnote

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.domain.model.ReminderType
import com.voicetasker.app.data.ai.GeminiService
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import com.voicetasker.app.util.FeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── ViewModel ──

data class AddNoteUiState(
    val title: String = "",
    val content: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val scheduledDate: Long = System.currentTimeMillis(),
    val selectedReminders: Set<ReminderType> = emptySet(),
    val isSaved: Boolean = false,
    val isAiProcessing: Boolean = false,
    val aiTitleSuggestion: String? = null
)

@HiltViewModel
class AddNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val feedbackManager: FeedbackManager,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddNoteUiState())
    val uiState: StateFlow<AddNoteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats, selectedCategoryId = it.selectedCategoryId ?: cats.firstOrNull()?.id) }
            }
        }
    }

    fun onTitleChanged(t: String) { _uiState.update { it.copy(title = t) } }
    fun onContentChanged(t: String) { _uiState.update { it.copy(content = t) } }
    fun onCategorySelected(id: Long) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun onDateChanged(d: Long) { _uiState.update { it.copy(scheduledDate = d) } }
    fun onReminderToggled(type: ReminderType) {
        _uiState.update { s ->
            val updated = s.selectedReminders.toMutableSet()
            if (type in updated) updated.remove(type) else updated.add(type)
            s.copy(selectedReminders = updated)
        }
    }

    fun requestAiSuggestions() {
        if (!geminiService.isAvailable) return
        val content = _uiState.value.content
        if (content.length < 15) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true) }
            // Generate title if empty
            if (_uiState.value.title.isBlank()) {
                val title = geminiService.generateTitle(content)
                if (title.isNotBlank()) _uiState.update { it.copy(title = title, aiTitleSuggestion = title) }
            }
            // Suggest category
            val catNames = _uiState.value.categories.map { it.name }
            val suggested = geminiService.suggestCategory(content, catNames)
            if (suggested != null) {
                val catId = _uiState.value.categories.find { it.name.equals(suggested, ignoreCase = true) }?.id
                if (catId != null) _uiState.update { it.copy(selectedCategoryId = catId) }
            }
            _uiState.update { it.copy(isAiProcessing = false) }
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            val s = _uiState.value
            val now = System.currentTimeMillis()
            val noteId = noteRepository.insertNote(
                Note(
                    title = s.title.ifBlank { "Nota manuale" },
                    transcription = s.content,
                    audioFilePath = "",
                    categoryId = s.selectedCategoryId ?: 1,
                    scheduledDate = s.scheduledDate,
                    createdAt = now,
                    updatedAt = now,
                    durationMs = 0
                )
            )
            s.selectedReminders.forEach { type ->
                reminderRepository.scheduleReminder(noteId, s.scheduledDate, type)
            }
            feedbackManager.play(FeedbackManager.FeedbackType.SAVE)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}

// ── Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ITALIAN)

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuova nota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::saveNote,
                        enabled = uiState.title.isNotBlank() || uiState.content.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Check, "Salva", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (uiState.aiTitleSuggestion != null) "Titolo (suggerito da AI ✨)" else "Titolo") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(Icons.Filled.Title, null) }
            )

            Spacer(Modifier.height(16.dp))

            // Content
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                label = { Text("Contenuto della nota") },
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(Icons.Filled.Notes, null) }
            )

            Spacer(Modifier.height(12.dp))

            // AI suggestion button
            if (uiState.content.length >= 15) {
                if (uiState.isAiProcessing) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer.copy(0.5f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("✨ Gemini sta analizzando...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = viewModel::requestAiSuggestions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("✨")
                        Spacer(Modifier.width(8.dp))
                        Text("Suggerisci con Gemini AI")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))

            // Category
            Text("Categoria" + if (uiState.aiTitleSuggestion != null) " (suggerita da AI ✨)" else "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                uiState.categories.forEach { cat ->
                    val c = try {
                        Color(android.graphics.Color.parseColor(cat.colorHex))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    val sel = uiState.selectedCategoryId == cat.id
                    Surface(
                        onClick = { viewModel.onCategorySelected(cat.id) },
                        shape = MaterialTheme.shapes.small,
                        color = if (sel) c.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) c else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Scheduled date
            Text("Data programmata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.CalendarMonth, null)
                Spacer(Modifier.width(8.dp))
                Text(df.format(Date(uiState.scheduledDate)))
            }

            Spacer(Modifier.height(20.dp))

            // Reminders
            Text("Promemoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ReminderType.entries.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = type in uiState.selectedReminders,
                        onCheckedChange = { viewModel.onReminderToggled(type) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(type.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Save button
            Button(
                onClick = viewModel::saveNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = uiState.title.isNotBlank() || uiState.content.isNotBlank()
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Salva nota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = uiState.scheduledDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dps.selectedDateMillis?.let { viewModel.onDateChanged(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) { DatePicker(state = dps) }
    }
}
