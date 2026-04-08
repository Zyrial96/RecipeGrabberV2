package com.recipegrabber.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recipegrabber.domain.llm.LlmModels
import com.recipegrabber.domain.llm.ProviderType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LlmSelectionStep(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingLlmViewModel = hiltViewModel()
) {
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val showKey by viewModel.showKey.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        StepHeader(
            icon = Icons.Default.Psychology,
            title = "KI-Anbieter wählen",
            description = "Wähle deinen bevorzugten KI-Anbieter und das Modell für die Rezept-Extraktion."
        )

        // Provider Selection
        Text(
            text = "Anbieter",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProviderType.entries.forEach { provider ->
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = { viewModel.onProviderSelected(provider) },
                    label = { Text(provider.name.replace("_", " ")) },
                    leadingIcon = if (selectedProvider == provider) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Model Selection
        Text(
            text = "Modell",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LlmModels.getModelsForProvider(selectedProvider).forEach { model ->
                FilterChip(
                    selected = selectedModel == model.id,
                    onClick = { viewModel.onModelSelected(model.id) },
                    label = { Text(model.name) }
                )
            }
        }

        selectedModel?.let { modelId ->
            LlmModels.getModelById(modelId)?.let { model ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(model.name, style = MaterialTheme.typography.titleSmall)
                        Text(model.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Kontext: ${model.contextWindow} Tokens",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Key Input
        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.onApiKeyChange(it) },
            label = { Text("${selectedProvider.name} API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            supportingText = {
                if (isValid) {
                    Text("✓ API Key gespeichert", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.toggleShowKey() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showKey) "Key verbergen" else "Key anzeigen")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation
        Button(
            onClick = onComplete,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abschließen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Zurück")
        }
    }
}