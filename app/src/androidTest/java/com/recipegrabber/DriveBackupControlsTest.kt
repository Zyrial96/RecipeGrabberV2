package com.recipegrabber

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.recipegrabber.data.remote.DriveArchive
import com.recipegrabber.presentation.ui.screens.DriveBackupControls
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import com.recipegrabber.presentation.viewmodel.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DriveBackupControlsTest {
    @get:Rule val compose = createComposeRule()
    private val archive = DriveArchive("fixture", "2026-10-13T12:00:00Z", 3)
    private var restored: DriveArchive? = null

    @Test fun restoreRequiresConfirmationAndCanBeCancelled() {
        render(SettingsUiState(driveBackupConnected = true, driveArchives = listOf(archive)))
        compose.onNodeWithText("Restore").performClick()
        assertEquals(null, restored)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, restored)
        compose.onNodeWithText("Restore").performClick()
        compose.onNodeWithText("Restore backup").performClick()
        assertEquals(archive, restored)
    }

    @Test fun busyOperationsDisableConcurrentActionsAndDisplayResult() {
        render(SettingsUiState(driveBackupConnected = true, driveBusy = true, driveStatus = "Backup saved: 3 recipes"))
        compose.onNodeWithText("Backup now").assertIsNotEnabled()
        compose.onNodeWithText("Refresh backups").assertIsNotEnabled()
        compose.onNodeWithText("Disconnect Drive").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Backup saved: 3 recipes").assertIsDisplayed()
    }

    @Test fun disconnectedStateOffersOnlyExplicitConnection() {
        render(SettingsUiState())
        compose.onNodeWithText("Connect Google Drive").assertIsDisplayed()
    }

    private fun render(state: SettingsUiState) {
        compose.setContent {
            RecipeGrabberTheme { DriveBackupControls(state, {}, {}, {}, {}, {}, { restored = it }) }
        }
    }
}
