package com.voicetasker.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.voicetasker.app.BuildConfig
import com.voicetasker.app.R
import com.voicetasker.app.ui.component.VoiceTaskerPremiumBanner
import com.voicetasker.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val purchaseCompleteMessage = stringResource(R.string.purchase_complete_welcome)

    // Purchase success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.billingState.purchaseSuccess) {
        if (uiState.billingState.purchaseSuccess) {
            snackbarHostState.showSnackbar(purchaseCompleteMessage)
            viewModel.clearPurchaseState()
        }
    }
    LaunchedEffect(uiState.billingState.purchaseError) {
        uiState.billingState.purchaseError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearPurchaseState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                .padding(
                    horizontal = VoiceTaskerSpacing.md,
                    vertical = VoiceTaskerSpacing.xs
                )
        ) {
            // ── Profile Section ──
            Text(
                text = stringResource(R.string.user_fallback),
                modifier = Modifier.padding(start = VoiceTaskerSpacing.xs),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(VoiceTaskerSpacing.xs))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large
            ) {
                if (uiState.isLoggedIn && uiState.userInfo != null) {
                    val user = uiState.userInfo!!
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            if (user.avatarUrl != null) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = stringResource(R.string.avatar),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            user.displayName?.firstOrNull()?.uppercase() ?: stringResource(R.string.user_initial_fallback),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    user.displayName ?: stringResource(R.string.user_fallback),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (uiState.billingState.isPremium) {
                                        VoiceTaskerDesign.colors.premiumContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (uiState.billingState.isPremium) {
                                            Icon(
                                                Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = VoiceTaskerDesign.colors.premiumGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = if (uiState.billingState.isPremium) {
                                                stringResource(R.string.premium)
                                            } else {
                                                stringResource(R.string.account_status_free)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.billingState.isPremium) {
                                                VoiceTaskerDesign.colors.premiumGold
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_out_account))
                        }
                    }
                } else {
                    // Not logged in
                    Column(
                        Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.not_signed_in),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Login, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_in), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Premium Section ──
            Text(
                text = stringResource(R.string.premium),
                modifier = Modifier.padding(start = VoiceTaskerSpacing.xs),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(VoiceTaskerSpacing.xs))
            if (!uiState.billingState.isPremium) {
                VoiceTaskerPremiumBanner(
                    title = stringResource(R.string.voicetasker_premium),
                    subtitle = stringResource(R.string.premium_trigger_default),
                    actionLabel = stringResource(R.string.upgrade_to_premium),
                    onAction = onNavigateToPaywall
                )
            } else {
                // Premium active
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = VoiceTaskerDesign.colors.premiumContainer
                    ),
                    border = BorderStroke(
                        1.dp,
                        VoiceTaskerDesign.colors.premiumGold.copy(alpha = 0.45f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Star, null,
                            tint = VoiceTaskerDesign.colors.premiumGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.premium_active_celebration),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        val subLabel = when (uiState.billingState.subscriptionType) {
                            "monthly" -> stringResource(R.string.monthly_subscription_active)
                            "yearly" -> stringResource(R.string.yearly_subscription_active)
                            "lifetime" -> stringResource(R.string.lifetime_license)
                            else -> stringResource(R.string.subscription_active)
                        }
                        Text(
                            subLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.information),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(VoiceTaskerSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xxs)
                ) {
                    Text(
                        stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.developed_in_italy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Debug section — only visible in debug builds
            if (com.voicetasker.app.BuildConfig.DEBUG) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(R.string.debug_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.simulate_premium),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(if (uiState.billingState.isPremium) R.string.debug_status_premium else R.string.debug_status_free),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.billingState.isPremium,
                            onCheckedChange = { viewModel.billingManager.debugTogglePremium() }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.debug_only_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.sign_out_account)) },
            text = { Text(stringResource(R.string.sign_out_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut()
                    }
                ) {
                    Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
