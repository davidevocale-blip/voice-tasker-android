package com.voicetasker.app.ui.screen.paywall

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    trigger: String,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.purchaseSuccess) {
        if (uiState.purchaseSuccess) {
            snackbarHostState.showSnackbar("🎉 Benvenuto in Premium!")
            viewModel.clearPurchaseState()
            onNavigateBack()
        }
    }
    LaunchedEffect(uiState.purchaseError) {
        uiState.purchaseError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearPurchaseState()
        }
    }

    val triggerMessage = when (trigger) {
        "note_limit" -> "Hai raggiunto il limite di 10 note gratuite"
        "ai_feature" -> "Funzionalità Premium"
        "reminder" -> "I promemoria sono una funzionalità Premium"
        "recording_limit" -> "Le registrazioni oltre 1 minuto sono Premium"
        else -> "Sblocca tutte le funzionalità"
    }

    val triggerIcon = when (trigger) {
        "note_limit" -> Icons.Filled.NoteAdd
        "ai_feature" -> Icons.Filled.AutoAwesome
        "reminder" -> Icons.Filled.Notifications
        "recording_limit" -> Icons.Filled.Mic
        else -> Icons.Filled.Star
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Premium Icon with glow
            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "alpha"
                )
                Box(
                    Modifier
                        .size(100.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.radialGradient(
                                listOf(Gold40.copy(glowAlpha), Gold40.copy(0.05f))
                            )
                        )
                )
                Icon(
                    Icons.Filled.Star, null,
                    tint = Gold40,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "VoiceTasker Premium",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Trigger message card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(triggerIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        triggerMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Features list
            val features = listOf(
                Triple(Icons.Filled.AllInclusive, "Note illimitate", "Crea tutte le note che vuoi"),
                Triple(Icons.Filled.Notifications, "Promemoria smart", "Reminder personalizzati"),
                Triple(Icons.Filled.Mic, "Registrazioni lunghe", "Fino a 10 minuti di registrazione")
            )

            features.forEach { (icon, title, subtitle) ->
                FeatureRow(icon = icon, title = title, subtitle = subtitle)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Purchase buttons
            if (!uiState.isLoggedIn) {
                // Not logged in — show login prompt
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.AccountCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Accedi per abbonarti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Devi effettuare l'accesso prima di acquistare Premium",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Login, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Accedi con Google", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Logged in — show purchase options
                // Monthly — highlighted
                Button(
                    onClick = {
                        val activity = context.findActivity() ?: return@Button
                        viewModel.launchMonthlyPurchase(activity)
                    },
                    Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple40),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !uiState.purchaseInProgress
                ) {
                    if (uiState.purchaseInProgress) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mensile — €3,99/mese", fontWeight = FontWeight.Bold)
                        Text("Cancella quando vuoi", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Yearly — best value
                OutlinedButton(
                    onClick = {
                        val activity = context.findActivity() ?: return@OutlinedButton
                        viewModel.launchYearlyPurchase(activity)
                    },
                    Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !uiState.purchaseInProgress
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Annuale — €29,99/anno")
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Mint40.copy(0.2f)
                            ) {
                                Text(
                                    " -37% ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Mint40,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "€2,50/mese • Risparmi €17,89",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Lifetime
                OutlinedButton(
                    onClick = {
                        val activity = context.findActivity() ?: return@OutlinedButton
                        viewModel.launchLifetimePurchase(activity)
                    },
                    Modifier.fillMaxWidth().height(48.dp),
                    enabled = !uiState.purchaseInProgress
                ) {
                    Text("Una tantum — €49,99 per sempre")
                }
            }

            Spacer(Modifier.height(16.dp))

            // "Not now" button
            TextButton(onClick = onNavigateBack) {
                Text("Non ora", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Purple40.copy(0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Purple40, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Check, null, tint = Mint40, modifier = Modifier.size(20.dp))
        }
    }
}
