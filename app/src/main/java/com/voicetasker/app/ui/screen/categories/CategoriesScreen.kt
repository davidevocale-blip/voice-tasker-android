package com.voicetasker.app.ui.screen.categories

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetasker.app.R
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.ui.resources.asString
import com.voicetasker.app.ui.resources.displayName
import com.voicetasker.app.ui.resources.persistedCategoryName
import com.voicetasker.app.ui.resources.UiText
import com.voicetasker.app.ui.theme.VoiceTaskerSizing
import com.voicetasker.app.ui.theme.VoiceTaskerSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val COLORS = listOf("#6C63FF", "#FF6584", "#00D9A6", "#FFB947", "#5BC0EB", "#E55934", "#9BC53D", "#FA7921", "#7768AE", "#3BCEAC", "#EE4266")

data class CatUiState(
    val categories: List<Category> = emptyList(),
    val showDialog: Boolean = false,
    val editing: Category? = null,
    val name: String = "",
    val originalPersistedName: String = "",
    val initialDisplayName: UiText? = null,
    val hasNameChanged: Boolean = false,
    val color: String = "#6C63FF",
    @StringRes val errorRes: Int? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val repo: CategoryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CatUiState())
    val uiState: StateFlow<CatUiState> = _uiState.asStateFlow()
    init { viewModelScope.launch { repo.getAllCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } } } }
    fun showAdd() { _uiState.update { it.copy(showDialog = true, editing = null, name = "", originalPersistedName = "", initialDisplayName = null, hasNameChanged = false, color = COLORS.random()) } }
    fun showEdit(c: Category, displayName: UiText) { _uiState.update { it.copy(showDialog = true, editing = c, name = "", originalPersistedName = c.name, initialDisplayName = displayName, hasNameChanged = false, color = c.colorHex) } }
    fun dismiss() { _uiState.update { it.copy(showDialog = false, errorRes = null) } }
    fun onNameChanged(n: String) {
        _uiState.update { it.copy(name = n, hasNameChanged = true) }
    }
    fun onColorChanged(c: String) { _uiState.update { it.copy(color = c) } }
    fun save() { val s = _uiState.value; val savedName = if (s.editing != null) persistedCategoryName(s.originalPersistedName, s.name, s.hasNameChanged) else s.name; if (savedName.isBlank()) { _uiState.update { it.copy(errorRes = R.string.category_name_required) }; return }; viewModelScope.launch { if (s.editing != null) repo.updateCategory(s.editing.copy(name = savedName, colorHex = s.color)) else repo.insertCategory(Category(name = savedName, colorHex = s.color, createdAt = System.currentTimeMillis())); dismiss() } }
    fun delete(id: Long) { viewModelScope.launch { repo.deleteCategoryById(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.categories_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAdd,
                modifier = Modifier.size(VoiceTaskerSizing.primaryFab),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_category)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = VoiceTaskerSpacing.md,
                end = VoiceTaskerSpacing.md,
                top = VoiceTaskerSpacing.sm,
                bottom = VoiceTaskerSpacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs)
        ) {
            items(uiState.categories, key = { it.id }) { category ->
                val categoryColor = categoryColor(category.colorHex)
                val displayName = category.displayName()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .heightIn(min = 72.dp)
                                .background(categoryColor)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = VoiceTaskerSpacing.sm)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = VoiceTaskerSpacing.sm)
                        ) {
                            Text(
                                text = displayName.asString(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (category.isDefault) {
                                Spacer(Modifier.height(VoiceTaskerSpacing.xxs))
                                Text(
                                    text = stringResource(
                                        R.string.default_category
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.showEdit(category, displayName) },
                            modifier = Modifier.size(
                                VoiceTaskerSizing.minimumTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit)
                            )
                        }
                        if (!category.isDefault) {
                            IconButton(
                                onClick = { viewModel.delete(category.id) },
                                modifier = Modifier.size(
                                    VoiceTaskerSizing.minimumTouchTarget
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(
                                        R.string.delete
                                    ),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Spacer(
                                Modifier.width(
                                    VoiceTaskerSizing.minimumTouchTarget
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.showDialog) {
        val initialDisplayName = uiState.initialDisplayName?.asString().orEmpty()
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = {
                Text(
                    stringResource(
                        if (uiState.editing != null) {
                            R.string.edit
                        } else {
                            R.string.new_category
                        }
                    )
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = if (uiState.hasNameChanged) uiState.name else initialDisplayName,
                        onValueChange = viewModel::onNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        isError = uiState.errorRes != null,
                        shape = MaterialTheme.shapes.medium
                    )
                    uiState.errorRes?.let { error ->
                        Spacer(Modifier.height(VoiceTaskerSpacing.xxs))
                        Text(
                            text = stringResource(error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(VoiceTaskerSpacing.md))
                    Text(
                        text = stringResource(R.string.category_color),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(
                            VoiceTaskerSpacing.xs
                        ),
                        contentPadding = PaddingValues(
                            vertical = VoiceTaskerSpacing.xxs
                        )
                    ) {
                        items(COLORS) { hex ->
                            val color = categoryColor(hex)
                            val selected = uiState.color == hex
                            Box(
                                modifier = Modifier
                                    .size(
                                        VoiceTaskerSizing.minimumTouchTarget
                                    )
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme
                                                    .onSurface,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        viewModel.onColorChanged(hex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (color.luminance() > 0.5f) {
                                            Color.Black
                                        } else {
                                            Color.White
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::save,
                    modifier = Modifier.heightIn(
                        min = VoiceTaskerSizing.minimumTouchTarget
                    )
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismiss,
                    modifier = Modifier.heightIn(
                        min = VoiceTaskerSizing.minimumTouchTarget
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun categoryColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
}
