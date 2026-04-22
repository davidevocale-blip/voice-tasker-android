package com.gentlefit.app.ui.screen.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.domain.model.NewsCategory
import com.gentlefit.app.ui.components.NewsCard
import com.gentlefit.app.ui.theme.*

@Composable
fun NewsScreen(
    onNavigateToAdmin: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: NewsViewModel = hiltViewModel()
) {
    val articles by viewModel.news.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val selectedArticle by viewModel.selectedArticle.collectAsState()

    if (selectedArticle != null) {
        NewsDetailView(article = selectedArticle!!, onBack = { viewModel.clearArticle() })
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .padding(horizontal = 24.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📰 News & Benessere", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = onNavigateToAdmin) {
                    Icon(Icons.Rounded.Settings, "Admin", tint = Plum40)
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = selectedCat == null, onClick = { viewModel.selectCategory(null) },
                        label = { Text("Tutti") }, shape = RoundedCornerShape(12.dp))
                }
                items(NewsCategory.entries.toList()) { cat ->
                    FilterChip(selected = selectedCat == cat, onClick = { viewModel.selectCategory(cat) },
                        label = { Text("${cat.emoji} ${cat.displayName}") }, shape = RoundedCornerShape(12.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        items(articles, key = { it.id }) { article ->
            NewsCard(article = article, onClick = { viewModel.selectArticle(article) })
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun NewsDetailView(article: com.gentlefit.app.domain.model.NewsArticle, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro")
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("${article.category.emoji} ${article.category.displayName}",
                style = MaterialTheme.typography.labelMedium, color = Plum40)
            Spacer(Modifier.height(8.dp))
            Text(article.title, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(article.publishedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(article.content, style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(40.dp))
        }
    }
}
