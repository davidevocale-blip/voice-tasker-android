package com.gentlefit.app.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.components.GentleButton
import com.gentlefit.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userGoal by viewModel.userGoal.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientPinkStart, GradientPinkEnd)))
            .padding(24.dp).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Page indicators
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(3) { index ->
                Box(
                    Modifier.padding(horizontal = 4.dp)
                        .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (index == currentPage) GentlePink50 else GentlePink50.copy(0.3f))
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        // Content
        AnimatedContent(targetState = currentPage, label = "onboarding") { page ->
            when (page) {
                0 -> OnboardingPage1()
                1 -> OnboardingPage2()
                2 -> OnboardingPage3(userName, userGoal,
                    onNameChange = { viewModel.updateName(it) },
                    onGoalChange = { viewModel.updateGoal(it) })
            }
        }

        Spacer(Modifier.weight(1f))

        // Buttons
        if (currentPage < 2) {
            GentleButton(text = "Avanti →", onClick = { viewModel.nextPage() })
            if (currentPage > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.previousPage() }) {
                    Text("← Indietro", color = GentlePink50)
                }
            }
        } else {
            GentleButton(
                text = "Inizia il tuo percorso 🌸",
                onClick = { viewModel.completeOnboarding(onComplete) },
                enabled = userName.isNotBlank()
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPage1() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🌸", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text("Benvenuta in\nGentleFit", style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = GentlePink30)
        Spacer(Modifier.height(16.dp))
        Text("Il benessere senza sforzo.\nNiente palestra hardcore, niente diete rigide.\nSolo piccoli gesti che fanno la differenza.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 26.sp)
    }
}

@Composable
private fun OnboardingPage2() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⏱️", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text("Solo 5 minuti\nal giorno", style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = GentlePink30)
        Spacer(Modifier.height(16.dp))
        Text("🧘 1 mini allenamento dolce\n🥗 1 micro-consiglio alimentare\n🎯 1 obiettivo semplice",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 30.sp)
        Spacer(Modifier.height(16.dp))
        Text("Tutto in formato amichevole,\ncome i consigli di un'amica. 💕",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = GentlePink50)
    }
}

@Composable
private fun OnboardingPage3(name: String, goal: String, onNameChange: (String) -> Unit, onGoalChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✨", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text("Parliamo di te", style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = GentlePink30)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Come ti chiami?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GentlePink50, cursorColor = GentlePink50)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = goal, onValueChange = onGoalChange,
            label = { Text("Il tuo obiettivo (facoltativo)") },
            placeholder = { Text("Es: sentirmi più energica") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GentlePink50, cursorColor = GentlePink50)
        )
    }
}
