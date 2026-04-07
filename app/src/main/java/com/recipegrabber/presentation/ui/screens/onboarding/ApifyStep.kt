package com.recipegrabber.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ApifyStep(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingApifyViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader(
            icon = Icons.Default.Key,
            title = "Apify Konfiguration",
            description = "Verbinde deinen Apify-Account, um TikTok und Instagram Videos zu verarbeiten."
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.onApiKeyChange(it) },
            label = { Text("Apify API Token") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Default.Help, contentDescription = "Hilfe")
                }
            },
            supportingText = {
                if (isValid) {
                    Text("✓ Gültiger Token", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { showKey = !showKey }) {
            Text(if (showKey) "Token verbergen" else "Token anzeigen")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Token wird überprüft...")
            }
            else -> {
                Button(
                    onClick = onNext,
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Weiter")
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
    }

    if (showHelpDialog) {
        ApifyHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun ApifyHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Help, contentDescription = null) },
        title = { Text("Wie bekomme ich meinen Apify Token?") },
        text = {
            Column {
                Text("1. Erstelle einen Account auf apify.com")
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Gehe in der Konsole zu 'Settings' → 'Integrations'")
                Spacer(modifier = Modifier.height(8.dp))
                Text("3. Kopiere den 'Personal API Token'")
                Spacer(modifier = Modifier.height(8.dp))
                Text("4. Füge ihn hier ein, um TikTok/Instagram-Videos verarbeiten zu können")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Verstanden")
            }
        }
    )
}