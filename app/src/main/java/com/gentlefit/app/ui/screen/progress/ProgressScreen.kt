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
import androidx.compose.ui.graphics.Color
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
fun ProgressScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))
        Text("📊 I tuoi progressi", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Come ti senti questa settimana?", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))
        ProgressChart(entries = state.entries)

        Spacer(Modifier.height(24.dp))

        if (!state.isSaved) {
            // Mood selector (dark card)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Plum20),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(20.dp)) {
                    EmotionSelector(selectedMood = state.selectedMood, onMoodSelected = { viewModel.selectMood(it) })

                    Spacer(Modifier.height(20.dp))

                    // Energy slider
                    Text("⚡ Livello di energia", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("😴", fontSize = 16.sp)
                        Slider(
                            value = state.energyLevel.toFloat(), onValueChange = { viewModel.setEnergy(it.toInt()) },
                            valueRange = 1f..5f, steps = 3, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = SageGreen50, activeTrackColor = SageGreen50,
                                inactiveTrackColor = Plum40)
                        )
                        Text("💪", fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Sleep slider
                    Text("😴 Qualità del sonno", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("😣", fontSize = 16.sp)
                        Slider(
                            value = state.sleepQuality.toFloat(), onValueChange = { viewModel.setSleep(it.toInt()) },
                            valueRange = 1f..5f, steps = 3, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = Lavender40, activeTrackColor = Lavender40,
                                inactiveTrackColor = Plum40)
                        )
                        Text("😊", fontSize = 16.sp)
                    }

                    // Optional weight
                    if (state.showWeight) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.weight, onValueChange = { viewModel.setWeight(it) },
                            label = { Text("Peso (kg) — facoltativo", color = Plum70) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Plum80,
                                focusedBorderColor = Plum60, unfocusedBorderColor = Plum40
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    GentleButton("Salva il mio stato 🌸",
                        onClick = { viewModel.saveProgress() },
                        enabled = state.selectedMood != null)
                }
            }
        } else {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SageGreen20)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅ Registrato per oggi!", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Torna domani per aggiornare i tuoi progressi", style = MaterialTheme.typography.bodySmall,
                        color = SageGreen70)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
