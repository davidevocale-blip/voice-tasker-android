package com.voicetasker.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.ui.component.VoiceTaskerCategoryChip
import com.voicetasker.app.ui.component.VoiceTaskerNoteCard
import com.voicetasker.app.ui.component.VoiceTaskerPremiumBanner
import com.voicetasker.app.ui.component.VoiceTaskerSearchField
import com.voicetasker.app.ui.component.VoiceTaskerStatePanel
import com.voicetasker.app.ui.component.VoiceTaskerStatePanelMode
import com.voicetasker.app.ui.theme.VoiceTaskerSizing
import com.voicetasker.app.ui.theme.VoiceTaskerSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRecord: () -> Unit,
    onNavigateToAddNote: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToPaywall: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.ITALIAN)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.sm)
            ) {
                SmallFloatingActionButton(
                    onClick = { if (uiState.isPremium || uiState.freeNotesRemaining > 0) onNavigateToAddNote() else onNavigateToPaywall("note_limit") },
                    modifier = Modifier.size(VoiceTaskerSizing.secondaryFab),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.manual_note_action_content_description)
                    )
                }
                FloatingActionButton(
                    onClick = { if (uiState.isPremium || uiState.freeNotesRemaining > 0) onNavigateToRecord() else onNavigateToPaywall("note_limit") },
                    modifier = Modifier.size(VoiceTaskerSizing.primaryFab),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.record)
                    )
                }
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
                top = VoiceTaskerSpacing.xs,
                end = VoiceTaskerSpacing.md,
                bottom = VoiceTaskerSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.sm)
        ) {
            if (!uiState.isPremium) {
                item {
                    VoiceTaskerPremiumBanner(
                        title = stringResource(R.string.upgrade_to_premium),
                        subtitle = pluralStringResource(
                            R.plurals.free_notes_remaining,
                            uiState.freeNotesRemaining,
                            uiState.freeNotesRemaining
                        ),
                        actionLabel = stringResource(R.string.upgrade),
                        onAction = { onNavigateToPaywall("upgrade") }
                    )
                }
            }

            item {
                VoiceTaskerSearchField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = stringResource(R.string.search_notes_hint),
                    searchContentDescription = stringResource(R.string.search)
                )
            }

            if (uiState.categories.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs)) {
                        items(uiState.categories) { cat ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            VoiceTaskerCategoryChip(
                                label = cat.name,
                                categoryColor = color,
                                selected = uiState.selectedCategoryId == cat.id,
                                onClick = { viewModel.onCategoryFilterChanged(cat.id) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    VoiceTaskerStatePanel(mode = VoiceTaskerStatePanelMode.Loading)
                }
            }

            if (uiState.notes.isEmpty() && !uiState.isLoading) {
                item {
                    VoiceTaskerStatePanel(
                        mode = VoiceTaskerStatePanelMode.Empty,
                        title = stringResource(R.string.no_notes),
                        message = stringResource(R.string.no_notes_hint),
                        icon = Icons.Outlined.MicNone
                    )
                }
            }

            items(uiState.notes, key = { it.id }) { note ->
                VoiceTaskerNoteCard(
                    title = note.title.ifBlank { stringResource(R.string.note) },
                    content = note.transcription.ifBlank { stringResource(R.string.no_content) },
                    categoryColor = viewModel.getCategoryColor(note.categoryId),
                    categoryName = viewModel.getCategoryName(note.categoryId),
                    dateTime = dateFormat.format(Date(note.scheduledDate)),
                    deleteContentDescription = stringResource(R.string.delete),
                    onDelete = { viewModel.deleteNote(note.id) },
                    onClick = { onNavigateToNoteDetail(note.id) },
                    modifier = Modifier.animateItem(fadeInSpec = tween(300), fadeOutSpec = tween(300))
                )
            }
            item { Spacer(Modifier.height(VoiceTaskerSpacing.huge)) }
        }
    }
}
