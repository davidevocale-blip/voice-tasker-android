package com.gentlefit.app.ui.screen.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.theme.*

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
    ) {
        // Header
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GentlePink80.copy(0.5f), MaterialTheme.colorScheme.background)))
                .padding(20.dp)
        ) {
            Column {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Indietro") }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(GentlePink50.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Text("🌸", fontSize = 28.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.userName.ifBlank { "Utente" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        if (state.userGoal.isNotBlank()) Text(state.userGoal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Stats
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCard("🔥", "${state.streakDays}", "Streak")
            StatCard("✅", "${state.completedDays}", "Completati")
        }

        Spacer(Modifier.height(16.dp))

        // Settings
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Impostazioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            SettingsToggle("Mostra peso", "Traccia il peso nei progressi", state.showWeight) { viewModel.toggleWeight(it) }
            SettingsToggle("Notifiche", "Promemoria giornalieri", state.notifications) { viewModel.toggleNotifications(it) }

            Spacer(Modifier.height(16.dp))

            // Invite friend
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SageGreen50.copy(0.1f))
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👯", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Invita un'amica", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Motivatevi a vicenda!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Prova GentleFit! L'app per il benessere senza sforzo 🌸 Insieme è più facile! 💕")
                        }
                        context.startActivity(Intent.createChooser(intent, "Invita un'amica"))
                    }) { Text("Invita", color = SageGreen50, fontWeight = FontWeight.SemiBold) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Premium CTA
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Lavender80.copy(0.5f))
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("💎", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Passa a Premium", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Sblocca tutti i contenuti", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onNavigateToPremium) { Text("Scopri", color = Lavender40, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String) {
    Card(
        Modifier.width(140.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = GentlePink50))
    }
}
