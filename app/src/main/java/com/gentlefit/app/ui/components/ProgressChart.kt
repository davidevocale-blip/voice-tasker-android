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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.domain.model.ProgressEntry
import com.gentlefit.app.ui.theme.Plum20
import com.gentlefit.app.ui.theme.Plum30
import com.gentlefit.app.ui.theme.Plum40
import com.gentlefit.app.ui.theme.Plum70
import com.gentlefit.app.ui.theme.Plum90
import com.gentlefit.app.ui.theme.SageGreen40
import com.gentlefit.app.ui.theme.SageGreen50
import com.gentlefit.app.ui.theme.Lavender40
import com.gentlefit.app.ui.theme.Lavender60
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressChart(entries: List<ProgressEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Plum20),
            contentAlignment = Alignment.Center
        ) {
            Text("📊 I tuoi progressi appariranno qui", style = MaterialTheme.typography.bodyMedium,
                color = Plum70)
        }
        return
    }

    // Build a calendar-like weekly view
    val today = LocalDate.now()
    val weekDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val entryByDate = entries.associateBy { it.date }

    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Plum20).padding(18.dp)
    ) {
        Text("📅 La tua settimana", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekDays.forEach { day ->
                val entry = entryByDate[day.toString()]
                val isToday = day == today
                val dayName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
                    .replaceFirstChar { it.uppercase() }.take(3)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    // Day label
                    Text(dayName, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) Color.White else Plum70,
                        fontSize = 10.sp)
                    Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) Color.White else Plum70,
                        fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))

                    // Bars
                    Box(Modifier.height(80.dp).width(28.dp), contentAlignment = Alignment.BottomCenter) {
                        if (entry != null) {
                            Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                                // Energy bar
                                Box(Modifier.width(12.dp).fillMaxHeight(entry.energyLevel / 5f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Brush.verticalGradient(listOf(SageGreen40, SageGreen50))))
                                // Sleep bar
                                Box(Modifier.width(12.dp).fillMaxHeight(entry.sleepQuality / 5f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Brush.verticalGradient(listOf(Lavender40, Lavender60))))
                            }
                        } else {
                            // Empty placeholder
                            Box(Modifier.width(28.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(Plum40.copy(0.3f)))
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Mood emoji
                    Text(entry?.mood?.emoji ?: "·", fontSize = 14.sp, textAlign = TextAlign.Center,
                        color = if (entry != null) Color.Unspecified else Plum40)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Legend
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = Plum70)
    }
}
