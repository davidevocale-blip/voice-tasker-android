package com.gentlefit.app.ui.screen.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.components.MicroGoalChip
import com.gentlefit.app.ui.theme.*

@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val active by viewModel.activeGoals.collectAsState()
    val completed by viewModel.completedGoals.collectAsState()
    val suggested by viewModel.suggestedGoals.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("🎯 Micro-obiettivi", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Piccoli gesti, grandi risultati", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }

        // Active goals
        if (active.isNotEmpty()) {
            item { Text("In corso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(active, key = { it.id }) { goal ->
                MicroGoalChip(title = goal.title, category = goal.category, isCompleted = false,
                    streakDays = goal.streakDays, onClick = { viewModel.completeGoal(goal.id) })
            }
        }

        // Completed
        if (completed.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("✅ Completati", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(completed.take(5), key = { it.id }) { goal ->
                MicroGoalChip(title = goal.title, category = goal.category, isCompleted = true,
                    streakDays = goal.streakDays, onClick = {})
            }
        }

        // Suggested
        item {
            Spacer(Modifier.height(16.dp))
            Text("💡 Suggeriti per te", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        items(suggested.take(6)) { goal ->
            Card(
                Modifier.fillMaxWidth().clickable { viewModel.addGoal(goal) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.category.emoji, modifier = Modifier.padding(end = 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(goal.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(goal.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("+ Aggiungi", style = MaterialTheme.typography.labelSmall, color = GentlePink50, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}
