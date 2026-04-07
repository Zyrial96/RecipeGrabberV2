package com.recipegrabber.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.presentation.ui.screens.RecipeDetailScreen
import com.recipegrabber.presentation.ui.screens.RecipeListScreen
import com.recipegrabber.presentation.ui.screens.SettingsScreen
import com.recipegrabber.presentation.ui.screens.onboarding.OnboardingScreen
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import com.recipegrabber.service.ClipboardMonitorService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkModeEnabled by preferencesRepository.darkModeEnabled.collectAsState(initial = true)
            val onboardingCompleted by preferencesRepository.onboardingCompleted.collectAsState(initial = false)

            RecipeGrabberTheme(darkTheme = darkModeEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    if (!onboardingCompleted) {
                        // Show Onboarding
                        OnboardingScreen(
                            onComplete = {
                                // Onboarding completed - restart to load main app
                                recreate()
                            }
                        )
                    } else {
                        // Main App Navigation
                        NavHost(
                            navController = navController,
                            startDestination = "recipe_list"
                        ) {
                            composable("recipe_list") {
                                RecipeListScreen(
                                    onRecipeClick = { recipeId ->
                                        navController.navigate("recipe_detail/$recipeId")
                                    },
                                    onSettingsClick = {
                                        navController.navigate("settings")
                                    }
                                )
                            }

                            composable(
                                route = "recipe_detail/{recipeId}",
                                arguments = listOf(
                                    navArgument("recipeId") { type = NavType.LongType }
                                )
                            ) {
                                RecipeDetailScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Only start clipboard monitor if onboarding is completed
        val onboardingCompleted = preferencesRepository.onboardingCompleted.value
        if (onboardingCompleted) {
            startClipboardMonitor()
        }
    }

    private fun startClipboardMonitor() {
        val serviceIntent = Intent(this, ClipboardMonitorService::class.java)
        startForegroundService(serviceIntent)
    }
}