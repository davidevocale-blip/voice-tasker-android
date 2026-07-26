package com.voicetasker.app.ui.screen.paywall

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.ui.resources.asString
import com.voicetasker.app.ui.theme.*
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
    val premiumWelcome = stringResource(R.string.premium_welcome)
    val purchaseError = uiState.purchaseError?.asString()

    LaunchedEffect(uiState.purchaseSuccess) {
        if (uiState.purchaseSuccess) {
            snackbarHostState.showSnackbar(premiumWelcome)
            viewModel.clearPurchaseState()
            onNavigateBack()
        }
    }
    LaunchedEffect(purchaseError) {
        purchaseError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearPurchaseState()
        }
    }

    val triggerMessage = when (trigger) {
        "note_limit" -> stringResource(R.string.premium_trigger_note_limit)
        "ai_feature" -> stringResource(R.string.premium_trigger_ai)
        "reminder" -> stringResource(R.string.premium_trigger_reminder)
        "recording_limit" -> stringResource(R.string.premium_trigger_recording_limit)
        else -> stringResource(R.string.premium_trigger_default)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                .padding(horizontal = VoiceTaskerSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.large,
                color = VoiceTaskerDesign.colors.premiumContainer,
                border = BorderStroke(
                    1.dp,
                    VoiceTaskerDesign.colors.premiumGold.copy(alpha = 0.45f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = VoiceTaskerDesign.colors.premiumGold,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.voicetasker_premium),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Trigger message card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Features list
            val features = listOf(
                Triple(Icons.Filled.AllInclusive, stringResource(R.string.feature_unlimited_notes), stringResource(R.string.feature_unlimited_notes_subtitle)),
                Triple(Icons.Filled.Notifications, stringResource(R.string.feature_smart_reminders), stringResource(R.string.feature_smart_reminders_subtitle)),
                Triple(Icons.Filled.Mic, stringResource(R.string.feature_long_recordings), stringResource(R.string.feature_long_recordings_subtitle))
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
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.AccountCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.sign_in_to_subscribe),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.sign_in_before_purchase),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Login, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_in_with_google), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Logged in — show purchase options
                // Monthly — highlighted
                PricingPlanCard(
                    title = stringResource(R.string.plan_name_monthly),
                    price = uiState.monthlyPrice,
                    unavailableLabel = stringResource(R.string.plan_unavailable),
                    available = uiState.isMonthlyAvailable,
                    purchaseInProgress = uiState.purchaseInProgress,
                    recommended = false,
                    onClick = {
                        val activity = context.findActivity() ?: return@PricingPlanCard
                        viewModel.launchMonthlyPurchase(activity)
                    }
                )

                Spacer(Modifier.height(10.dp))

                // Yearly — best value
                PricingPlanCard(
                    title = stringResource(R.string.plan_name_yearly),
                    price = uiState.yearlyPrice,
                    unavailableLabel = stringResource(R.string.plan_unavailable),
                    available = uiState.isYearlyAvailable,
                    purchaseInProgress = uiState.purchaseInProgress,
                    recommended = true,
                    onClick = {
                        val activity = context.findActivity() ?: return@PricingPlanCard
                        viewModel.launchYearlyPurchase(activity)
                    }
                )

                Spacer(Modifier.height(10.dp))

                // Lifetime
                PricingPlanCard(
                    title = stringResource(R.string.plan_name_lifetime),
                    price = uiState.lifetimePrice,
                    unavailableLabel = stringResource(R.string.plan_unavailable),
                    available = uiState.isLifetimeAvailable,
                    purchaseInProgress = uiState.purchaseInProgress,
                    recommended = false,
                    onClick = {
                        val activity = context.findActivity() ?: return@PricingPlanCard
                        viewModel.launchLifetimePurchase(activity)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // "Not now" button
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.heightIn(min = VoiceTaskerSizing.minimumTouchTarget)
            ) {
                Text(stringResource(R.string.not_now), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PricingPlanCard(
    title: String,
    price: String?,
    unavailableLabel: String,
    available: Boolean,
    purchaseInProgress: Boolean,
    recommended: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        !available -> MaterialTheme.colorScheme.outlineVariant
        recommended -> VoiceTaskerDesign.colors.premiumGold
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = if (recommended && available) {
        VoiceTaskerDesign.colors.premiumContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        enabled = available && !purchaseInProgress,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(
            horizontal = VoiceTaskerSpacing.md,
            vertical = VoiceTaskerSpacing.sm
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (available) price.orEmpty() else unavailableLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(VoiceTaskerSizing.minimumTouchTarget)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(VoiceTaskerSpacing.xs))
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = VoiceTaskerDesign.colors.success,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
