package com.gentlefit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.domain.model.ProgressEntry
import com.gentlefit.app.ui.theme.SageGreen50
import com.gentlefit.app.ui.theme.Lavender40

@Composable
fun ProgressChart(entries: List<ProgressEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text("📊 I tuoi progressi appariranno qui", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp)
    ) {
        Text("La tua settimana", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            entries.takeLast(7).forEach { entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(Modifier.width(12.dp).height((entry.energyLevel / 5f * 60).dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(SageGreen50))
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.width(12.dp).height((entry.sleepQuality / 5f * 60).dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(Lavender40.copy(alpha = 0.7f)))
                    Spacer(Modifier.height(4.dp))
                    Text(entry.mood.emoji, fontSize = 12.sp)
                    Text(entry.date.takeLast(2), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LegendItem(SageGreen50, "Energia")
            Spacer(Modifier.width(16.dp))
            LegendItem(Lavender40, "Sonno")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
