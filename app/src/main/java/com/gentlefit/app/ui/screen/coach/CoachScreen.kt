package com.gentlefit.app.ui.screen.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlefit.app.ui.components.CoachBubble
import com.gentlefit.app.ui.theme.*

@Composable
fun CoachScreen(viewModel: CoachViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val lastMessage = messages.lastOrNull()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GentlePink80.copy(0.5f), MaterialTheme.colorScheme.background)))
                .padding(20.dp)
        ) {
            Column {
                Text("💬 Coach Marta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("La tua amica del benessere", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Messages
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                CoachBubble(text = msg.text, type = msg.type)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Quick replies
        if (lastMessage?.quickReplies?.isNotEmpty() == true) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                lastMessage.quickReplies.forEach { reply ->
                    OutlinedButton(
                        onClick = { viewModel.sendQuickReply(reply) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GentlePink50),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(GentlePink50, GentlePink40))
                        )
                    ) { Text(reply, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
