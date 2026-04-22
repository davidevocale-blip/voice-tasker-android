package com.gentlefit.app.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.components.*
import com.gentlefit.app.ui.theme.*
import java.time.LocalTime

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val hour = LocalTime.now().hour
    val greeting = when { hour < 12 -> "Buongiorno"; hour < 17 -> "Buon pomeriggio"; else -> "Buonasera" }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .verticalScroll(scrollState).padding(horizontal = 24.dp).statusBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))

            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("$greeting,", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.userName.ifBlank { "cara" }} 🌸", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onNavigateToProfile) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Plum60.copy(0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, "Profilo", tint = Plum40)
                    }
                }
            }

            // Streak
            if (state.streakDays > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF6B35).copy(0.1f), Color(0xFFFFD700).copy(0.1f))))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(18.dp), tint = Color(0xFFFF6B35))
                    Spacer(Modifier.width(4.dp))
                    Text("${state.streakDays} giorni di fila!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF6B35))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick Stats + Weekly Ring (dark card)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Plum20)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    WeeklyRing(progress = state.weeklyCompletion, size = 90.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickStatRow("⚡", "Energia", "${String.format("%.1f", state.averageEnergy)}/5")
                        QuickStatRow("😴", "Sonno", "${String.format("%.1f", state.averageSleep)}/5")
                        QuickStatRow("🔥", "Streak", "${state.streakDays} gg")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Mood Trend (dark card)
            if (state.recentMoods.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Plum30)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Umore ultimi giorni", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, color = Plum90)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.recentMoods.takeLast(7).forEach { mood ->
                                val emoji = when (mood.uppercase()) {
                                    "GREAT" -> "😊"; "GOOD" -> "🙂"; "LOW" -> "😔"
                                    "STRONG" -> "💪"; "PEACEFUL" -> "🌸"; else -> "😐"
                                }
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Daily routine
            val routine = state.routine
            if (routine != null) {
                DailyProgressBar(completedCount = routine.completionCount)
                Spacer(Modifier.height(16.dp))
                Text("La tua routine di oggi", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(12.dp))

                RoutineCard(emoji = routine.exercise.type.emoji, title = routine.exercise.title,
                    description = routine.exercise.description, duration = "${routine.exercise.durationMinutes} min",
                    isCompleted = routine.isExerciseCompleted, onAction = { viewModel.completeExercise() })
                Spacer(Modifier.height(10.dp))
                RoutineCard(emoji = "🥗", title = "Consiglio alimentare", description = routine.foodTip.text,
                    isCompleted = routine.isFoodTipFollowed, onAction = { viewModel.completeFoodTip() })
                Spacer(Modifier.height(10.dp))
                RoutineCard(emoji = "🎯", title = "Obiettivo del giorno", description = routine.dailyGoal.text,
                    isCompleted = routine.isGoalCompleted, onAction = { viewModel.completeGoal() })
            } else if (!state.isLoading) {
                // Motivational daily phrase (dark card)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Plum20)) {
                    Column(Modifier.padding(24.dp)) {
                        Text("✨", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(state.quote, style = MaterialTheme.typography.bodyLarge,
                            color = Color.White, lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Celebration overlay
        if (state.showCelebration) {
            CelebrationBurst(show = true, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun QuickStatRow(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Plum70)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
