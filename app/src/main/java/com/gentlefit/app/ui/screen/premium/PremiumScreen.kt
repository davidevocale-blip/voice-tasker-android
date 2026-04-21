package com.gentlefit.app.ui.screen.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.ui.components.GentleButton
import com.gentlefit.app.ui.theme.*

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF2E2E50), Color(0xFF1A1A2E)))
        ).verticalScroll(rememberScrollState()).padding(24.dp).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text("💎", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("GentleFit Premium", style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        Spacer(Modifier.height(4.dp))
        Text("Il meglio per il tuo benessere", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

        Spacer(Modifier.height(32.dp))

        val features = listOf(
            "🧘" to "Programmi personalizzati" to "Menopausa, gonfiore, schiena, postura",
            "🥗" to "Piani alimentari soft" to "Non diete rigide, ma consigli mirati",
            "🎥" to "Video extra esclusivi" to "Yoga dolce, relax guidato, postura",
            "⏰" to "Reminder intelligenti" to "Promemoria al momento giusto",
            "👯" to "Community privata" to "Connettiti con altre donne del percorso",
            "📊" to "Report dettagliati" to "Analisi approfondite dei tuoi progressi"
        )

        features.forEach { (iconTitle, desc) ->
            val (icon, title) = iconTitle
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Price
        Text("a partire da", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("€5", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
            Text("/mese", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.6f), modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(Modifier.height(20.dp))
        GentleButton("Prova Premium gratis per 7 giorni ✨", onClick = { /* TODO: billing */ })
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Torna indietro", color = Color.White.copy(0.5f)) }
        Spacer(Modifier.height(24.dp))
    }
}
