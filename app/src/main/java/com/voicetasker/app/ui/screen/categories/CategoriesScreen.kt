package com.voicetasker.app.ui.screen.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val COLORS = listOf("#6C63FF", "#FF6584", "#00D9A6", "#FFB947", "#5BC0EB", "#E55934", "#9BC53D", "#FA7921", "#7768AE", "#3BCEAC", "#EE4266")

data class CatUiState(val categories: List<Category> = emptyList(), val showDialog: Boolean = false, val editing: Category? = null, val name: String = "", val color: String = "#6C63FF", val error: String? = null)

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val repo: CategoryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CatUiState())
    val uiState: StateFlow<CatUiState> = _uiState.asStateFlow()
    init { viewModelScope.launch { repo.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } } }
    fun showAdd() { _uiState.update { it.copy(showDialog = true, editing = null, name = "", color = COLORS.random()) } }
    fun showEdit(c: Category) { _uiState.update { it.copy(showDialog = true, editing = c, name = c.name, color = c.colorHex) } }
    fun dismiss() { _uiState.update { it.copy(showDialog = false, error = null) } }
    fun onNameChanged(n: String) { _uiState.update { it.copy(name = n) } }
    fun onColorChanged(c: String) { _uiState.update { it.copy(color = c) } }
    fun save() { val s = _uiState.value; if (s.name.isBlank()) { _uiState.update { it.copy(error = "Nome richiesto") }; return }; viewModelScope.launch { if (s.editing != null) repo.updateCategory(s.editing.copy(name = s.name, colorHex = s.color)) else repo.insertCategory(Category(name = s.name, colorHex = s.color, createdAt = System.currentTimeMillis())); dismiss() } }
    fun delete(id: Long) { viewModelScope.launch { repo.deleteCategoryById(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Categorie", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        floatingActionButton = { FloatingActionButton(onClick = viewModel::showAdd, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Add, "Aggiungi") } },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.categories) { cat ->
                val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Label, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); if (cat.isDefault) Text("Predefinita", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (!cat.isDefault) { IconButton(onClick = { viewModel.showEdit(cat) }) { Icon(Icons.Filled.Edit, "Modifica", Modifier.size(20.dp)) }; IconButton(onClick = { viewModel.delete(cat.id) }) { Icon(Icons.Filled.Delete, "Elimina", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) } }
                    }
                }
            }
        }
    }
    if (uiState.showDialog) {
        AlertDialog(onDismissRequest = viewModel::dismiss, title = { Text(if (uiState.editing != null) "Modifica" else "Nuova categoria") },
            text = { Column {
                OutlinedTextField(uiState.name, viewModel::onNameChanged, Modifier.fillMaxWidth(), label = { Text("Nome") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                uiState.error?.let { Spacer(Modifier.height(4.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp)); Text("Colore", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(COLORS) { hex -> val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }; Box(Modifier.size(36.dp).clip(CircleShape).background(c).clickable { viewModel.onColorChanged(hex) }, contentAlignment = Alignment.Center) { if (uiState.color == hex) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) } } }
            } },
            confirmButton = { TextButton(onClick = viewModel::save) { Text("Salva") } },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("Annulla") } })
    }
}
