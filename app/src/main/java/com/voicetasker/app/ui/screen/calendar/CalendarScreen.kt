package com.voicetasker.app.ui.screen.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.domain.model.Note
import com.voicetasker.app.ui.localization.firstDayOfWeek
import com.voicetasker.app.ui.localization.orderedShortWeekdayNames
import com.voicetasker.app.ui.localization.resourceLocale
import com.voicetasker.app.ui.resources.asString
import com.voicetasker.app.ui.resources.displayName
import com.voicetasker.app.ui.theme.VoiceTaskerSizing
import com.voicetasker.app.ui.theme.VoiceTaskerSpacing
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    onNavigateToNoteDetail: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = resourceLocale()
    val monthFormatter = remember(locale) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"),
            locale
        )
    }
    val today = Calendar.getInstance(locale)
    val selectedCalendar = Calendar.getInstance(locale).apply {
        timeInMillis = uiState.selectedDate
    }
    val currentMonth = uiState.currentMonth
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = Calendar.getInstance(locale).apply {
        set(
            currentMonth.get(Calendar.YEAR),
            currentMonth.get(Calendar.MONTH),
            1
        )
    }.get(Calendar.DAY_OF_WEEK)
    val startOffset = Math.floorMod(
        firstDayOfMonth - firstDayOfWeek(locale),
        7
    )
    val dayNames = remember(locale) { orderedShortWeekdayNames(locale) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = VoiceTaskerSpacing.xs,
                end = VoiceTaskerSpacing.xs,
                bottom = VoiceTaskerSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xs)
        ) {
            item {
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
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = VoiceTaskerSpacing.xxs,
                            vertical = VoiceTaskerSpacing.sm
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.onMonthChanged(-1) },
                                modifier = Modifier.size(
                                    VoiceTaskerSizing.minimumTouchTarget
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled
                                        .KeyboardArrowLeft,
                                    contentDescription = stringResource(
                                        R.string.previous_month
                                    )
                                )
                            }
                            Text(
                                text = monthFormatter
                                    .format(currentMonth.time),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { viewModel.onMonthChanged(1) },
                                modifier = Modifier.size(
                                    VoiceTaskerSizing.minimumTouchTarget
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled
                                        .KeyboardArrowRight,
                                    contentDescription = stringResource(
                                        R.string.next_month
                                    )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Row(Modifier.widthIn(min = 336.dp)) {
                                dayNames.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .padding(vertical = VoiceTaskerSpacing.xs),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme
                                            .onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            var day = 1
                            val rows = (startOffset + daysInMonth + 6) / 7
                            for (row in 0 until rows) {
                                Row(Modifier.widthIn(min = 336.dp)) {
                                    for (column in 0 until 7) {
                                        val index = row * 7 + column
                                        if (index < startOffset || day > daysInMonth) {
                                            Spacer(
                                                Modifier.size(
                                                    VoiceTaskerSizing
                                                        .minimumTouchTarget
                                                )
                                            )
                                        } else {
                                            val currentDay = day
                                            val isToday =
                                                currentDay == today.get(
                                                    Calendar.DAY_OF_MONTH
                                                ) &&
                                                    currentMonth.get(
                                                        Calendar.MONTH
                                                    ) == today.get(
                                                        Calendar.MONTH
                                                    ) &&
                                                    currentMonth.get(
                                                        Calendar.YEAR
                                                    ) == today.get(
                                                        Calendar.YEAR
                                                    )
                                            val isSelected =
                                                currentDay == selectedCalendar.get(
                                                    Calendar.DAY_OF_MONTH
                                                ) &&
                                                    currentMonth.get(
                                                        Calendar.MONTH
                                                    ) == selectedCalendar.get(
                                                        Calendar.MONTH
                                                    ) &&
                                                    currentMonth.get(
                                                        Calendar.YEAR
                                                    ) == selectedCalendar.get(
                                                        Calendar.YEAR
                                                    )

                                            CalendarDay(
                                                day = currentDay,
                                                isToday = isToday,
                                                isSelected = isSelected,
                                                hasNotes = currentDay in
                                                    uiState.daysWithNotes,
                                                onClick = {
                                                    val calendar =
                                                        Calendar.getInstance(
                                                            locale
                                                        )
                                                    calendar.set(
                                                        currentMonth.get(
                                                            Calendar.YEAR
                                                        ),
                                                        currentMonth.get(
                                                            Calendar.MONTH
                                                        ),
                                                        currentDay,
                                                        0,
                                                        0,
                                                        0
                                                    )
                                                    viewModel.onDateSelected(
                                                        calendar.timeInMillis
                                                    )
                                                }
                                            )
                                            day++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                Text(
                    text = stringResource(R.string.notes_for_day),
                    modifier = Modifier.padding(horizontal = VoiceTaskerSpacing.xs),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (uiState.notesForDate.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_commitments),
                        modifier = Modifier.padding(
                            horizontal = VoiceTaskerSpacing.xs,
                            vertical = VoiceTaskerSpacing.md
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.notesForDate, key = { it.id }) { note ->
                    CalendarNoteCard(
                        note = note,
                        categoryName = viewModel.getCategory(note.categoryId)
                            ?.displayName()
                            ?.asString()
                            .orEmpty(),
                        categoryColor = viewModel.getCategoryColor(note.categoryId),
                        onClick = { onNavigateToNoteDetail(note.id) },
                        modifier = Modifier.padding(
                            horizontal = VoiceTaskerSpacing.xs
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasNotes: Boolean,
    onClick: () -> Unit
) {
    val dayModifier = when {
        isSelected -> Modifier.background(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        )
        isToday -> Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        )
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(VoiceTaskerSizing.minimumTouchTarget)
            .then(dayModifier)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasNotes) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                )
            } else {
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarNoteCard(
    note: Note,
    categoryName: String,
    categoryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(4.dp)
                    .heightIn(min = 112.dp)
                    .background(categoryColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(VoiceTaskerSpacing.sm)
            ) {
                Text(
                    text = note.title.ifBlank {
                        stringResource(R.string.voice_note)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.xxs))
                Text(
                    text = note.transcription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(VoiceTaskerSpacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        VoiceTaskerSpacing.xs
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        VoiceTaskerSpacing.xxs
                    )
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = VoiceTaskerSpacing.xs,
                                vertical = VoiceTaskerSpacing.xxs
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(Modifier.width(VoiceTaskerSpacing.xxs))
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (note.noteTime.isNotBlank()) {
                        MetadataItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            text = note.noteTime
                        )
                    }
                    if (note.location.isNotBlank()) {
                        MetadataItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            text = note.location
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = VoiceTaskerSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(VoiceTaskerSpacing.xxs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
