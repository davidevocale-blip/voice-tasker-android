package com.voicetasker.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicetasker.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Impostazioni", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            // Premium
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Star, null, tint = Gold40, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(8.dp)); Text("VoiceTasker Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    listOf("Note vocali illimitate", "Registrazioni fino a 10 min", "Categorie illimitate", "Reminder personalizzati", "Nessuna pubblicità").forEach { Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Check, null, tint = Mint40, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(it, style = MaterialTheme.typography.bodyMedium) } }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {}, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple40), shape = MaterialTheme.shapes.medium) { Text("Mensile — €3,99/mese", fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {}, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Text("Annuale — €29,99/anno") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {}, Modifier.fillMaxWidth()) { Text("Lifetime — €49,99") }
                }
            }
            Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
            Text("Informazioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp))
            Text("VoiceTasker v1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Sviluppato con ❤️ in Italia", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
