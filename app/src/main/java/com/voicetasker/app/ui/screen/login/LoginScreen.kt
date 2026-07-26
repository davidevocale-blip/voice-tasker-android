package com.voicetasker.app.ui.screen.login

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetasker.app.R
import com.voicetasker.app.ui.resources.asString
import com.voicetasker.app.ui.theme.*
import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest
import android.os.Build
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun getAppSignatureSHA1(context: Context): String {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signatures = packageInfo.signingInfo?.apkContentsSigners
            if (!signatures.isNullOrEmpty()) {
                val md = MessageDigest.getInstance("SHA-1")
                md.update(signatures[0].toByteArray())
                return md.digest().joinToString(":") { String.format("%02X", it) }
            }
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            val signatures = packageInfo.signatures
            if (!signatures.isNullOrEmpty()) {
                val md = MessageDigest.getInstance("SHA-1")
                md.update(signatures[0].toByteArray())
                return md.digest().joinToString(":") { String.format("%02X", it) }
            }
        }
    } catch (e: Exception) {
        return context.getString(R.string.signature_error, e.message)
    }
    return context.getString(R.string.signature_not_found)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = VoiceTaskerSpacing.md,
                    end = VoiceTaskerSpacing.md,
                    bottom = VoiceTaskerSpacing.xxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    // Features list
                    val features: List<Pair<androidx.compose.ui.graphics.vector.ImageVector, String>> = listOf(
                        Pair(Icons.Filled.AllInclusive, stringResource(R.string.feature_unlimited_voice_notes)),
                        Pair(Icons.Filled.Timer, stringResource(R.string.feature_ten_minute_recordings)),
                        Pair(Icons.Filled.Category, stringResource(R.string.feature_unlimited_categories)),
                        Pair(Icons.Filled.NotificationsActive, stringResource(R.string.feature_custom_reminders)),
                        Pair(Icons.Filled.Block, stringResource(R.string.feature_no_ads))
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            features.forEach { (icon, text) ->
                                Row(
                                    Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon, null,
                                        tint = VoiceTaskerDesign.colors.success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Error message
                    uiState.errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                error.asString(),
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Success message
                    uiState.showSuccessMessage?.let { success ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = VoiceTaskerDesign.colors.successContainer
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                success.asString(),
                                modifier = Modifier.padding(12.dp),
                                color = VoiceTaskerDesign.colors.success,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    AnimatedVisibility(visible = uiState.isLoading) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = VoiceTaskerSpacing.md),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(VoiceTaskerSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.sm)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = stringResource(R.string.please_wait),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    AuthModeSelector(
                        isLoginMode = uiState.isLoginMode,
                        onLoginSelected = {
                            if (!uiState.isLoginMode) viewModel.toggleLoginMode()
                        },
                        onRegisterSelected = {
                            if (uiState.isLoginMode) viewModel.toggleLoginMode()
                        }
                    )

                    Spacer(Modifier.height(VoiceTaskerSpacing.md))

                    // Email & Password Fields
                    OutlinedTextField(
                        value = uiState.emailInput,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = VoiceTaskerSizing.inputMinimumHeight),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    // Email Login/Register Button
                    Button(
                        onClick = viewModel::submitEmailAuth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
                        enabled = !uiState.isLoading,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(if (uiState.isLoginMode) R.string.sign_in_with_email else R.string.register))
                    }
                    
                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = viewModel::toggleLoginMode,
                        modifier = Modifier.heightIn(min = VoiceTaskerSizing.minimumTouchTarget)
                    ) {
                        Text(stringResource(if (uiState.isLoginMode) R.string.no_account_register else R.string.already_have_account_sign_in))
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.or),
                            modifier = Modifier.padding(horizontal = VoiceTaskerSpacing.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    // Google Sign-In button
                    Button(
                        onClick = {
                            val activity = context.findActivity() ?: return@Button
                            viewModel.signInWithGoogle(activity)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            stringResource(R.string.google_logo_letter),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.continue_with_google),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.terms_acceptance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    TextButton(
                        onClick = { showDiagnostics = true },
                        modifier = Modifier.heightIn(min = VoiceTaskerSizing.minimumTouchTarget)
                    ) {
                        Text(stringResource(R.string.network_diagnostics))
                    }
                }
            }
        }
    }

    if (showDiagnostics) {
        AlertDialog(
            onDismissRequest = { showDiagnostics = false },
            title = { Text(stringResource(R.string.diagnostic_report)) },
            text = {
                Column {
                    Text(stringResource(R.string.installed_sha1), fontWeight = FontWeight.Bold)
                    Text(getAppSignatureSHA1(context), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.package_label), fontWeight = FontWeight.Bold)
                    Text(context.packageName, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnostics = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun AuthModeSelector(
    isLoginMode: Boolean,
    onLoginSelected: () -> Unit,
    onRegisterSelected: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(VoiceTaskerSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(VoiceTaskerSpacing.xxs)
        ) {
            AuthModeButton(
                label = stringResource(R.string.sign_in),
                selected = isLoginMode,
                onClick = onLoginSelected,
                modifier = Modifier.weight(1f)
            )
            AuthModeButton(
                label = stringResource(R.string.register),
                selected = !isLoginMode,
                onClick = onRegisterSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = VoiceTaskerSpacing.xs)
        ) {
            Text(label, maxLines = 2, textAlign = TextAlign.Center)
        }
    } else {
        TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = VoiceTaskerSizing.minimumTouchTarget),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = VoiceTaskerSpacing.xs)
        ) {
            Text(label, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}
