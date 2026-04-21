package com.gentlefit.app.ui.screen.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.components.EmotionSelector
import com.gentlefit.app.ui.components.GentleButton
import com.gentlefit.app.ui.components.ProgressChart
import com.gentlefit.app.ui.theme.*

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))
        Text("📊 I tuoi progressi", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Come ti senti questa settimana?", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))
        ProgressChart(entries = state.entries)

        Spacer(Modifier.height(24.dp))

        if (!state.isSaved) {
            // Mood selector
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(16.dp)) {
                    EmotionSelector(selectedMood = state.selectedMood, onMoodSelected = { viewModel.selectMood(it) })

                    Spacer(Modifier.height(20.dp))

                    // Energy slider
                    Text("⚡ Livello di energia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("😴", fontSize = 16.sp)
                        Slider(
                            value = state.energyLevel.toFloat(), onValueChange = { viewModel.setEnergy(it.toInt()) },
                            valueRange = 1f..5f, steps = 3, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = SageGreen50, activeTrackColor = SageGreen50)
                        )
                        Text("💪", fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Sleep slider
                    Text("😴 Qualità del sonno", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("😣", fontSize = 16.sp)
                        Slider(
                            value = state.sleepQuality.toFloat(), onValueChange = { viewModel.setSleep(it.toInt()) },
                            valueRange = 1f..5f, steps = 3, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = Lavender40, activeTrackColor = Lavender40)
                        )
                        Text("😊", fontSize = 16.sp)
                    }

                    // Optional weight
                    if (state.showWeight) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.weight, onValueChange = { viewModel.setWeight(it) },
                            label = { Text("Peso (kg) — facoltativo") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    GentleButton("Salva il mio stato 🌸",
                        onClick = { viewModel.saveProgress() },
                        enabled = state.selectedMood != null)
                }
            }
        } else {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(SuccessGreen.copy(0.1f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅ Registrato per oggi!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Torna domani per aggiornare i tuoi progressi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}
