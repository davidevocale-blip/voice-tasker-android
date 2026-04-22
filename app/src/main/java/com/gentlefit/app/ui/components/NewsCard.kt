package com.gentlefit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlefit.app.domain.model.NewsArticle
import com.gentlefit.app.ui.theme.Plum20
import com.gentlefit.app.ui.theme.Plum50
import com.gentlefit.app.ui.theme.Plum70
import com.gentlefit.app.ui.theme.Plum80

@Composable
fun NewsCard(article: NewsArticle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Plum20),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row {
                Text(article.category.emoji, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(article.category.displayName, style = MaterialTheme.typography.labelSmall, color = Plum50)
                Spacer(Modifier.weight(1f))
                if (!article.isRead) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Plum50))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(article.title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(article.summary, style = MaterialTheme.typography.bodySmall,
                color = Plum70, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(article.publishedDate, style = MaterialTheme.typography.labelSmall, color = Plum80.copy(alpha = 0.6f))
        }
    }
}
