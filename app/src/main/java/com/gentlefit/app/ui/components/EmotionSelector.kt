package com.gentlefit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.domain.model.Mood
import com.gentlefit.app.ui.theme.GentlePink50

@Composable
fun EmotionSelector(
    selectedMood: Mood?,
    onMoodSelected: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Come ti senti oggi?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Mood.entries.forEach { mood ->
                val isSelected = selectedMood == mood
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onMoodSelected(mood) }) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(if (isSelected) GentlePink50.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                        contentAlignment = Alignment.Center
                    ) { Text(mood.emoji, fontSize = 24.sp) }
                    Spacer(Modifier.height(4.dp))
                    Text(mood.label, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) GentlePink50 else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp)
                }
            }
        }
    }
}
