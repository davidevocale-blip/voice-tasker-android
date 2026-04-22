package com.gentlefit.app.ui.screen.coach

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.theme.*

@Composable
fun CoachScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: CoachViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // Header (dark card)
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Plum20)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Plum40),
                    contentAlignment = Alignment.Center
                ) { Text("🌸", fontSize = 36.sp) }
                Spacer(Modifier.height(12.dp))
                Text("Coach Marta", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("La tua amica del benessere", style = MaterialTheme.typography.bodySmall,
                    color = Plum70)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Greeting (dark card)
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Plum30)
        ) {
            Text(
                state.greeting.ifBlank { "Ciao! Come stai oggi?" },
                Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge,
                color = Color.White, lineHeight = 24.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // Response area (dark card)
        AnimatedVisibility(
            visible = state.currentResponse != null,
            enter = fadeIn(tween(500)) + expandVertically(tween(400)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            Column {
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SageGreen20)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("💬 ${state.selectedOption ?: ""}", style = MaterialTheme.typography.labelMedium,
                            color = Plum80, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(state.currentResponse ?: "", style = MaterialTheme.typography.bodyLarge,
                            color = Color.White, lineHeight = 24.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { viewModel.resetOptions() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("← Altre opzioni", color = Plum40, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Options
        AnimatedVisibility(
            visible = state.showOptions,
            enter = fadeIn(tween(400)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Come ti senti oggi?", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                state.options.forEach { option ->
                    Button(
                        onClick = { viewModel.selectOption(option) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Plum30, contentColor = Color.White)
                    ) {
                        Text(option, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
    }
}
