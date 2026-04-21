package com.gentlefit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.domain.model.GoalCategory
import com.gentlefit.app.ui.theme.GentlePink50
import com.gentlefit.app.ui.theme.SageGreen50
import com.gentlefit.app.ui.theme.SuccessGreen
import com.gentlefit.app.ui.theme.WarmCream50

@Composable
fun MicroGoalChip(
    title: String,
    category: GoalCategory,
    isCompleted: Boolean,
    streakDays: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isCompleted) SuccessGreen.copy(alpha = 0.12f)
        else categoryColor(category).copy(alpha = 0.1f),
        label = "bgColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isCompleted) SuccessGreen.copy(alpha = 0.3f)
                else categoryColor(category).copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(categoryColor(category).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = category.emoji, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
            if (streakDays > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFF6B35)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$streakDays giorni",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF6B35)
                    )
                }
            }
        }

        if (isCompleted) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Completato",
                tint = SuccessGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun categoryColor(category: GoalCategory): Color = when (category) {
    GoalCategory.ACQUA -> Color(0xFF64B5F6)
    GoalCategory.MOVIMENTO -> SageGreen50
    GoalCategory.RELAX -> GentlePink50
    GoalCategory.ALIMENTAZIONE -> WarmCream50
}
