package com.gentlefit.app.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.components.DailyProgressBar
import com.gentlefit.app.ui.components.RoutineCard
import com.gentlefit.app.ui.theme.*
import java.time.LocalTime

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Buongiorno"
        hour < 17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("$greeting,", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${state.userName.ifBlank { "cara" }} 🌸",
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onNavigateToProfile) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(GentlePink50.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Person, "Profilo", tint = GentlePink50) }
            }
        }

        // Streak
        if (state.streakDays > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(
                        androidx.compose.ui.graphics.Color(0xFFFF6B35).copy(0.1f),
                        androidx.compose.ui.graphics.Color(0xFFFFD700).copy(0.1f)
                    ))).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color(0xFFFF6B35))
                Spacer(Modifier.width(4.dp))
                Text("${state.streakDays} giorni di fila!", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFFF6B35))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quote
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Lavender80, GentlePink80.copy(0.5f))))
                .padding(16.dp)
        ) {
            Text(state.quote, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(0.8f), lineHeight = 22.sp)
        }

        Spacer(Modifier.height(20.dp))

        // Daily progress
        val routine = state.routine
        if (routine != null) {
            DailyProgressBar(completedCount = routine.completionCount)
            Spacer(Modifier.height(16.dp))

            Text("La tua routine di oggi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            RoutineCard(
                emoji = routine.exercise.type.emoji,
                title = routine.exercise.title,
                description = routine.exercise.description,
                duration = "${routine.exercise.durationMinutes} min",
                isCompleted = routine.isExerciseCompleted,
                onAction = { viewModel.completeExercise() }
            )
            Spacer(Modifier.height(10.dp))
            RoutineCard(
                emoji = "🥗", title = "Consiglio alimentare",
                description = routine.foodTip.text,
                isCompleted = routine.isFoodTipFollowed,
                onAction = { viewModel.completeFoodTip() }
            )
            Spacer(Modifier.height(10.dp))
            RoutineCard(
                emoji = "🎯", title = "Obiettivo del giorno",
                description = routine.dailyGoal.text,
                isCompleted = routine.isGoalCompleted,
                onAction = { viewModel.completeGoal() }
            )
        } else if (!state.isLoading) {
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌿 La tua routine sta arrivando...", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}
