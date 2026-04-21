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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.domain.model.MessageType
import com.gentlefit.app.ui.theme.GentlePink50
import com.gentlefit.app.ui.theme.GentlePink80
import com.gentlefit.app.ui.theme.SageGreen50
import com.gentlefit.app.ui.theme.Lavender60

@Composable
fun CoachBubble(text: String, type: MessageType, modifier: Modifier = Modifier) {
    val isUser = type == MessageType.USER
    val isCelebration = type == MessageType.CELEBRATION

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(GentlePink50.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Text(if (isCelebration) "🎉" else "🌸", fontSize = 18.sp) }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier.widthIn(max = 280.dp).clip(
                RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp, bottomEnd = 16.dp
                )
            ).background(
                when {
                    isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isCelebration -> Brush.linearGradient(listOf(SageGreen50.copy(0.2f), Lavender60.copy(0.2f)))
                    else -> Brush.linearGradient(listOf(GentlePink80.copy(0.3f), GentlePink80.copy(0.15f)))
                }
            ).padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}
