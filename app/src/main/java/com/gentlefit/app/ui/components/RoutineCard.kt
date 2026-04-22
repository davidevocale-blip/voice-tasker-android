package com.gentlefit.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.ui.theme.Plum20
import com.gentlefit.app.ui.theme.Plum30
import com.gentlefit.app.ui.theme.Plum40
import com.gentlefit.app.ui.theme.Plum70
import com.gentlefit.app.ui.theme.SageGreen20
import com.gentlefit.app.ui.theme.SageGreen50
import com.gentlefit.app.ui.theme.SageGreen70
import com.gentlefit.app.ui.theme.SuccessGreen

@Composable
fun RoutineCard(
    emoji: String,
    title: String,
    description: String,
    duration: String? = null,
    isCompleted: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) SageGreen20 else Plum20
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isCompleted) { onAction() }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) SuccessGreen.copy(alpha = 0.3f)
                        else Plum40.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCompleted) "✅" else emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) SageGreen70 else Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) SageGreen50 else Plum70,
                    maxLines = 2
                )
                if (duration != null && !isCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⏱️ $duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = Plum70
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action icon
            if (!isCompleted) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Plum40, Plum30))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Inizia",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Completato",
                    tint = SuccessGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun DailyProgressBar(
    completedCount: Int,
    totalCount: Int = 3,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Plum30)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progresso di oggi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "$completedCount/$totalCount completati",
                    style = MaterialTheme.typography.labelMedium,
                    color = Plum70
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (completedCount == totalCount) SuccessGreen else Plum40,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
            if (completedCount == totalCount) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🎉 Fantastico! Hai completato tutto oggi!",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
