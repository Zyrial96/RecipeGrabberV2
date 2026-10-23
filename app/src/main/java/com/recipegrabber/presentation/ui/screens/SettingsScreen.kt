package com.recipegrabber.presentation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.recipegrabber.domain.llm.GeminiAuthMode
import com.recipegrabber.domain.llm.LlmModels
import com.recipegrabber.domain.llm.ProviderType
import com.recipegrabber.presentation.viewmodel.SettingsUiState
import com.recipegrabber.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.finishDriveAuthorization(result.resultCode, result.data)
    }
    val googleAiAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleGoogleAiAuthorizationResult(result.resultCode, result.data)
    }
    val googleFilesAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleGoogleAiAuthorizationResult(result.resultCode, result.data, includeFiles = true)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.credentialsUnavailable) {
                Text(
                    "Saved API keys are unavailable. Enter them again or restart to retry migration.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // AI Provider Section
            SettingsSection(title = "AI Provider", icon = Icons.Default.SmartToy) {
                AiProviderSettings(
                    uiState = uiState,
                    viewModel = viewModel,
                    onGoogleFilesSignIn = {
                        viewModel.startGoogleAiAuthorization(includeFiles = true) { pendingIntent ->
                            googleFilesAuthorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        }
                    },
                    onGoogleAiSignIn = {
                        viewModel.startGoogleAiAuthorization { pendingIntent ->
                            googleAiAuthorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        }
                    }
                )
            }

            // Apify Section
            SettingsSection(title = "Video Scraping (Apify)", icon = Icons.Default.Link) {
                ApifySettings(uiState, viewModel)
            }

            // Google Drive Section
            SettingsSection(title = "Google Drive Backups", icon = Icons.Default.CloudSync) {
                DriveSettings(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDriveSignIn = {
                        viewModel.connectDrive { pendingIntent ->
                            driveSignInLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                        }
                    }
                )
            }

            // Clipboard Section
            SettingsSection(title = "Clipboard Monitor", icon = Icons.Default.ContentPaste) {
                ClipboardSettings(uiState, viewModel)
            }

            // Appearance Section
            SettingsSection(title = "Appearance", icon = Icons.Default.DarkMode) {
                AppearanceSettings(uiState, viewModel)
            }

            SettingsSection(title = "Debug Logs", icon = Icons.Default.BugReport) {
                OutlinedButton(
                    onClick = onNavigateToLogs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View & Export Logs")
                }
            }
        }
    }

    // Apify Help Dialog
    if (uiState.showApifyHelp) {
        ApifyHelpDialog(onDismiss = { viewModel.dismissApifyHelp() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiProviderSettings(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onGoogleAiSignIn: () -> Unit,
    onGoogleFilesSignIn: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }
    var showFilesConsent by remember { mutableStateOf(false) }

    if (showFilesConsent) {
        AlertDialog(
            onDismissRequest = { showFilesConsent = false },
            title = { Text("Optional Google permission") },
            text = { Text("Google requests access to view Cloud Storage data for video uploads. This additional permission is not required for YouTube or small videos.") },
            confirmButton = {
                TextButton(onClick = {
                    showFilesConsent = false
                    onGoogleFilesSignIn()
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showFilesConsent = false }) { Text("Cancel") }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Provider Selection
        Text(
            "Select Provider",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProviderType.entries.forEach { provider ->
                FilterChip(
                    selected = uiState.llmProvider == provider,
                    onClick = { viewModel.setLlmProvider(provider) },
                    label = { Text(provider.name.replace("_", " ")) },
                    leadingIcon = if (uiState.llmProvider == provider) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }

        // Model Selection
        Text(
            "Select Model",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LlmModels.getModelsForProvider(uiState.llmProvider).forEach { model ->
                FilterChip(
                    selected = uiState.llmModel == model.id,
                    onClick = { viewModel.setLlmModel(model.id) },
                    label = { Text(model.name) }
                )
            }
        }

        // Model Info
        LlmModels.getModelById(uiState.llmModel)?.let { model ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(model.name, style = MaterialTheme.typography.bodyMedium)
                    Text(model.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (uiState.llmProvider == ProviderType.GEMINI) {
            Text(
                "Authentication",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = uiState.geminiAuthMode == GeminiAuthMode.API_KEY,
                    onClick = { viewModel.setGeminiAuthMode(GeminiAuthMode.API_KEY) },
                    label = { Text("API Key") },
                    leadingIcon = if (uiState.geminiAuthMode == GeminiAuthMode.API_KEY) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
                FilterChip(
                    selected = uiState.geminiAuthMode == GeminiAuthMode.GOOGLE_OAUTH,
                    onClick = { viewModel.setGeminiAuthMode(GeminiAuthMode.GOOGLE_OAUTH) },
                    label = { Text("Google OAuth") },
                    leadingIcon = if (uiState.geminiAuthMode == GeminiAuthMode.GOOGLE_OAUTH) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }

        if (uiState.llmProvider == ProviderType.GEMINI && uiState.geminiAuthMode == GeminiAuthMode.GOOGLE_OAUTH) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (uiState.geminiOAuthAuthorized) {
                            "Google OAuth is connected for Gemini."
                        } else {
                            "Connect a Google account to authorize Gemini without an API key."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onGoogleAiSignIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.geminiOAuthAuthorized) "Reconnect Google OAuth" else "Connect Google OAuth")
                    }
                    OutlinedButton(
                        onClick = { showFilesConsent = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Authorize video uploads")
                    }
                }
            }
            return@Column
        }

        // API Key Input
        val apiKey = when (uiState.llmProvider) {
            ProviderType.OPENAI -> uiState.openAiApiKey
            ProviderType.GEMINI -> uiState.geminiApiKey
            ProviderType.CLAUDE -> uiState.claudeApiKey
            ProviderType.KIMI -> uiState.kimiApiKey
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { 
                when (uiState.llmProvider) {
                    ProviderType.OPENAI -> viewModel.setOpenAiApiKey(it)
                    ProviderType.GEMINI -> viewModel.setGeminiApiKey(it)
                    ProviderType.CLAUDE -> viewModel.setClaudeApiKey(it)
                    ProviderType.KIMI -> viewModel.setKimiApiKey(it)
                }
            },
            label = { Text("${uiState.llmProvider.name} API Key") },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { showApiKey = !showApiKey }) {
            Text(if (showApiKey) "Hide API Key" else "Show API Key")
        }
    }
}

@Composable
fun ApifySettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    var showKey by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Apify is used to download TikTok and Instagram videos for processing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = uiState.apifyApiKey,
            onValueChange = { viewModel.setApifyApiKey(it) },
            label = { Text("Apify API Token") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = { viewModel.showApifyHelp() }) {
                    Icon(Icons.Default.Help, contentDescription = "Help")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { showKey = !showKey }) {
                Text(if (showKey) "Hide Token" else "Show Token")
            }

            TextButton(onClick = { viewModel.showApifyHelp() }) {
                Icon(Icons.Default.Help, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("How to get token")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DriveSettings(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onDriveSignIn: () -> Unit
) = DriveBackupControls(uiState, onDriveSignIn, viewModel::backupToDrive, viewModel::refreshDriveBackups,
    viewModel::disconnectDrive, viewModel::cancelDriveOperation, viewModel::restoreDriveBackup)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DriveBackupControls(
    uiState: SettingsUiState,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onCancel: () -> Unit,
    onRestore: (com.recipegrabber.data.remote.DriveArchive) -> Unit
) {
    var restoreArchive by remember { mutableStateOf<com.recipegrabber.data.remote.DriveArchive?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (uiState.driveBackupConnected) {
            Text(uiState.driveBackupAccount.ifBlank { "Google Drive connected" }, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBackup, enabled = !uiState.driveBusy) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup now")
                }
                OutlinedButton(onClick = onRefresh, enabled = !uiState.driveBusy) { Text("Refresh backups") }
                TextButton(onClick = onDisconnect, enabled = !uiState.driveBusy) { Text("Disconnect Drive") }
            }
            if (uiState.driveArchives.isEmpty() && !uiState.driveBusy) Text("No backups loaded")
            uiState.driveArchives.forEach { archive ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${archive.createdTime}\n${archive.recipeCount} recipes", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { restoreArchive = archive }, enabled = !uiState.driveBusy) { Text("Restore") }
                }
            }
        } else {
            OutlinedButton(onClick = onConnect, enabled = !uiState.driveBusy) { Text("Connect Google Drive") }
        }
        if (uiState.driveBusy) {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        uiState.driveStatus?.let {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
    restoreArchive?.let { archive ->
        AlertDialog(
            onDismissRequest = { restoreArchive = null },
            title = { Text("Restore backup?") },
            text = { Text("Add missing recipes from this backup? Existing recipes and local changes will be kept.") },
            confirmButton = { TextButton(onClick = { restoreArchive = null; onRestore(archive) }) { Text("Restore backup") } },
            dismissButton = { TextButton(onClick = { restoreArchive = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ClipboardSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsToggle(
            icon = Icons.Default.ContentPaste,
            title = "Monitor Clipboard",
            subtitle = "Auto-detect video URLs",
            checked = uiState.clipboardMonitorEnabled,
            onCheckedChange = { viewModel.setClipboardMonitorEnabled(it) }
        )

        SettingsToggle(
            icon = Icons.Default.Science,
            title = "Auto-extract Recipes",
            subtitle = "Automatically extract when URL detected",
            checked = uiState.autoExtractRecipes,
            onCheckedChange = { viewModel.setAutoExtractRecipes(it) }
        )
    }
}

@Composable
fun AppearanceSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsToggle(
        icon = Icons.Default.DarkMode,
        title = "Dark Mode",
        subtitle = if (uiState.darkModeEnabled) "Dark theme enabled" else "Light theme enabled",
        checked = uiState.darkModeEnabled,
        onCheckedChange = { viewModel.setDarkModeEnabled(it) }
    )
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
fun ApifyHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Help, contentDescription = null) },
        title = { Text("How to get your Apify Token") },
        text = {
            Column {
                Text("1. Create an account on apify.com")
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Go to Console → 'Settings' → 'Integrations'")
                Spacer(modifier = Modifier.height(8.dp))
                Text("3. Copy the 'Personal API Token'")
                Spacer(modifier = Modifier.height(8.dp))
                Text("4. Paste it here to process TikTok/Instagram videos")
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        "Apify is used to download videos from TikTok and Instagram since direct scraping is blocked by these platforms.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
