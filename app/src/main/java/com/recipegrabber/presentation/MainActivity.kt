package com.recipegrabber.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.presentation.ui.screens.LogViewerScreen
import com.recipegrabber.presentation.ui.screens.RecipeDetailScreen
import com.recipegrabber.presentation.ui.screens.RecipeExtractionBottomSheet
import com.recipegrabber.presentation.ui.screens.RecipeListScreen
import com.recipegrabber.presentation.ui.screens.SettingsScreen
import com.recipegrabber.presentation.ui.screens.onboarding.OnboardingScreen
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import com.recipegrabber.service.ClipboardMonitorService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicReference

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val pendingVideoUrl = AtomicReference<String?>(null)
    private val clipboardReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ClipboardMonitorService.ACTION_RECIPE_URL_DETECTED) {
                val url = intent.getStringExtra(ClipboardMonitorService.EXTRA_VIDEO_URL)
                url?.let {
                    pendingVideoUrl.set(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle share intent
        handleShareIntent(intent)

        setContent {
            val darkModeEnabled by preferencesRepository.darkModeEnabled.collectAsState(initial = true)
            val onboardingCompleted by preferencesRepository.onboardingCompleted.collectAsState(initial = false)
            
            var showExtractionSheet by remember { mutableStateOf(false) }
            var extractionUrl by remember { mutableStateOf("") }

            val url = pendingVideoUrl.getAndSet(null)
            if (url != null) {
                extractionUrl = url
                showExtractionSheet = true
            }

            RecipeGrabberTheme(darkTheme = darkModeEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    if (!onboardingCompleted) {
                        OnboardingScreen(
                            onComplete = {
                                recreate()
                            }
                        )
                    } else {
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
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToLogs = { navController.navigate("logs") }
                                )
                            }

                            composable("logs") {
                                LogViewerScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Extraction Bottom Sheet
                        if (showExtractionSheet && extractionUrl.isNotBlank()) {
                            RecipeExtractionBottomSheet(
                                videoUrl = extractionUrl,
                                onDismiss = {
                                    showExtractionSheet = false
                                    extractionUrl = ""
                                },
                                onRecipeSaved = { recipeId ->
                                    showExtractionSheet = false
                                    extractionUrl = ""
                                    navController.navigate("recipe_detail/$recipeId")
                                },
                                onNavigateToSettings = {
                                    showExtractionSheet = false
                                    extractionUrl = ""
                                    navController.navigate("settings")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            sharedText?.let { text ->
                if (isValidVideoUrl(text)) {
                    pendingVideoUrl.set(text)
                }
            }
        }
    }

    private fun isValidVideoUrl(url: String): Boolean {
        val videoPatterns = listOf(
            "youtube.com/watch",
            "youtu.be/",
            "tiktok.com/",
            "instagram.com/",
            "facebook.com/",
            "twitter.com/",
            "x.com/",
            "vimeo.com/",
            "dailymotion.com/"
        )
        return videoPatterns.any { url.contains(it, ignoreCase = true) }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val onboardingCompleted = preferencesRepository.onboardingCompleted.first()
            if (onboardingCompleted) {
                startClipboardMonitor()
                registerClipboardReceiver()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterClipboardReceiver()
    }

    private fun startClipboardMonitor() {
        val serviceIntent = Intent(this, ClipboardMonitorService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun registerClipboardReceiver() {
        val filter = IntentFilter(ClipboardMonitorService.ACTION_RECIPE_URL_DETECTED)
        registerReceiver(clipboardReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterClipboardReceiver() {
        try {
            unregisterReceiver(clipboardReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
        }
    }
}