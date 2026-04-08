package com.recipegrabber.presentation.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Provider Section
            SettingsSection(title = "AI Provider", icon = Icons.Default.SmartToy) {
                AiProviderSettings(uiState, viewModel)
            }

            // Apify Section
            SettingsSection(title = "Video Scraping (Apify)", icon = Icons.Default.Link) {
                ApifySettings(uiState, viewModel)
            }

            // Google Drive Section
            SettingsSection(title = "Google Drive Sync", icon = Icons.Default.CloudSync) {
                DriveSettings(uiState, viewModel)
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
fun AiProviderSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    var showApiKey by remember { mutableStateOf(false) }

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
fun DriveSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsToggle(
            icon = Icons.Default.CloudSync,
            title = "Enable Drive Sync",
            subtitle = if (uiState.driveSyncEnabled && uiState.googleAccountEmail.isNotEmpty()) {
                "Connected to ${uiState.googleAccountEmail}"
            } else {
                "Backup recipes to Google Drive"
            },
            checked = uiState.driveSyncEnabled,
            onCheckedChange = { viewModel.setDriveSyncEnabled(it) }
        )

        if (uiState.driveSyncEnabled) {
            OutlinedButton(
                onClick = { viewModel.signOutGoogle() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Disconnect")
            }
        }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
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
        modifier = Modifier.fillMaxWidth(),
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
            onCheckedChange = onCheckedChange
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