package com.recipegrabber.domain.usecase

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.apify.ApifyService
import com.recipegrabber.data.remote.apify.ScrapedVideoData
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.GeminiAuthMode
import com.recipegrabber.domain.llm.LlmProviderFactory
import com.recipegrabber.domain.llm.ProviderType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URI

data class ProgressUpdate(
    val step: String,
    val message: String,
    val progress: Float
)

@Singleton
class ExtractRecipeUseCase @Inject constructor(
    private val apifyService: ApifyService,
    private val llmProviderFactory: LlmProviderFactory,
    private val preferencesRepository: PreferencesRepository,
    private val saveRecipeUseCase: SaveRecipeUseCase,
    private val logger: AppLogger
) {

    sealed class ExtractionResult {
        data class Success(val recipe: Recipe) : ExtractionResult()
        data class Error(val message: String) : ExtractionResult()
        data class ScrapingFailed(val message: String) : ExtractionResult()
        data class NoApiKey(val provider: String) : ExtractionResult()
    }

    private val _progress = MutableStateFlow(ProgressUpdate("idle", "Preparing...", 0f))
    val progress: StateFlow<ProgressUpdate> = _progress.asStateFlow()

    suspend fun invokeWithProgress(videoUrl: String, useApify: Boolean = true): ExtractionResult {
        val uri = try {
            URI(videoUrl).also {
                require(it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() && it.userInfo == null)
            }
        } catch (e: Exception) {
            return ExtractionResult.Error("Bitte einen gueltigen HTTPS-Videolink ohne Zugangsdaten eingeben.")
        }
        _progress.value = ProgressUpdate("detecting", "Erkenne Plattform...", 0.05f)
        logger.i("ExtractRecipe", "Starting video extraction")
        val platform = detectPlatform(uri)
        val platformName = platform.name.lowercase().replaceFirstChar { it.uppercase() }
        logger.d("ExtractRecipe", "Detected platform: $platform")
        
        val apifyKey = preferencesRepository.apifyApiKey.first()
        
        val scrapedData: ScrapedVideoData? = if (useApify && apifyKey.isNotBlank() && (platform == Platform.TIKTOK || platform == Platform.INSTAGRAM)) {
            _progress.value = ProgressUpdate("scraping", "Video wird von $platformName heruntergeladen...", 0.15f)
            logger.i("ExtractRecipe", "Attempting Apify scrape for $platform")
            val result = scrapeWithApify(videoUrl, apifyKey)
            if (result == null && (platform == Platform.TIKTOK || platform == Platform.INSTAGRAM)) {
                logger.w("ExtractRecipe", "Apify scraping failed for $platform")
                return ExtractionResult.ScrapingFailed("Scraping failed for $platform content. The video will be analyzed directly by the LLM.")
            }
            logger.i("ExtractRecipe", "Apify scrape ${if (result != null) "succeeded" else "returned null"}")
            result
        } else {
            null
        }

        val providerType = preferencesRepository.llmProviderType.first()
        val providerName = when (providerType) {
            ProviderType.OPENAI -> "OpenAI"
            ProviderType.GEMINI -> "Gemini"
            ProviderType.CLAUDE -> "Claude"
            ProviderType.KIMI -> "Kimi"
        }
        _progress.value = ProgressUpdate("connecting", "Verbinde mit $providerName...", 0.3f)

        val provider = llmProviderFactory.create(providerType)
        logger.i("ExtractRecipe", "Using LLM provider: $providerType")

        val hasApiKey = when (providerType) {
            ProviderType.OPENAI -> preferencesRepository.openAiApiKey.first().isNotBlank()
            ProviderType.GEMINI -> when (preferencesRepository.geminiAuthMode.first()) {
                GeminiAuthMode.API_KEY -> preferencesRepository.geminiApiKey.first().isNotBlank()
                GeminiAuthMode.GOOGLE_OAUTH -> true
            }
            ProviderType.CLAUDE -> preferencesRepository.claudeApiKey.first().isNotBlank()
            ProviderType.KIMI -> preferencesRepository.kimiApiKey.first().isNotBlank()
        }

        if (!hasApiKey) {
            logger.e("ExtractRecipe", "No API key for provider: $providerType")
            return ExtractionResult.NoApiKey(providerType.name)
        }

        _progress.value = ProgressUpdate("analyzing", "$providerName analysiert das Video...", 0.5f)

        return try {
            val contextUrl = scrapedData?.videoUrl ?: videoUrl
            val description = scrapedData?.description ?: ""

            val result = provider.extractRecipeFromVideo(contextUrl)
            
            _progress.value = ProgressUpdate("parsing", "Rezept wird erstellt...", 0.8f)
            
            result.fold(
                onSuccess = { extractedRecipe ->
                    require(extractedRecipe.title.isNotBlank() &&
                        extractedRecipe.servings > 0 &&
                        extractedRecipe.prepTimeMinutes >= 0 && extractedRecipe.cookTimeMinutes >= 0 &&
                        extractedRecipe.ingredients.isNotEmpty() &&
                        extractedRecipe.ingredients.all {
                            it.name.isNotBlank() && (it.amount == null || (it.amount.isFinite() && it.amount >= 0))
                        } &&
                        extractedRecipe.steps.isNotEmpty() &&
                        extractedRecipe.steps.all {
                            it.instruction.isNotBlank() && (it.duration == null || it.duration >= 0)
                        }) {
                        "Die KI hat kein vollstaendiges Rezept geliefert."
                    }
                    val enhancedRecipe = extractedRecipe.copy(
                        sourceUrl = videoUrl,
                        sourceType = "VIDEO",
                        description = extractedRecipe.description.ifBlank { description }
                    ).also {
                        it.ingredients = extractedRecipe.ingredients
                        it.steps = extractedRecipe.steps
                    }
                    
                    _progress.value = ProgressUpdate("saving", "Rezept wird gespeichert...", 0.9f)
                    val savedId = saveRecipeUseCase(enhancedRecipe)
                    logger.i("ExtractRecipe", "Recipe saved with ID: $savedId")
                    _progress.value = ProgressUpdate("done", "Fertig!", 1.0f)
                    ExtractionResult.Success(enhancedRecipe.copy(id = savedId).also {
                        it.ingredients = enhancedRecipe.ingredients
                        it.steps = enhancedRecipe.steps
                    })
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    logger.e("ExtractRecipe", "LLM extraction failed: ${error.message}", error)
                    ExtractionResult.Error(error.message ?: "Extraction failed")
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("ExtractRecipe", "Unexpected error", e)
            ExtractionResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend operator fun invoke(videoUrl: String): ExtractionResult = invokeWithProgress(videoUrl)

    suspend fun invokeDirect(videoUrl: String): ExtractionResult = invokeWithProgress(videoUrl, useApify = false)

    private suspend fun scrapeWithApify(url: String, apiKey: String): ScrapedVideoData? {
        return try {
            val result = when (detectPlatform(URI(url))) {
                Platform.TIKTOK -> apifyService.scrapeTikTokVideo(url, apiKey)
                Platform.INSTAGRAM -> apifyService.scrapeInstagramReel(url, apiKey)
                else -> return null
            }
            result.getOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("ExtractRecipe", "Apify scrape exception: ${e.message}", e)
            null
        }
    }

    private fun detectPlatform(uri: URI): Platform {
        val host = uri.host.orEmpty().lowercase(java.util.Locale.ROOT)
        fun matches(domain: String) = host == domain || host.endsWith(".$domain")
        return when {
            matches("tiktok.com") -> Platform.TIKTOK
            matches("instagram.com") -> Platform.INSTAGRAM
            matches("youtube.com") || matches("youtu.be") -> Platform.YOUTUBE
            matches("facebook.com") -> Platform.FACEBOOK
            matches("twitter.com") || matches("x.com") -> Platform.TWITTER
            else -> Platform.UNKNOWN
        }
    }

    private enum class Platform {
        TIKTOK, INSTAGRAM, YOUTUBE, FACEBOOK, TWITTER, UNKNOWN
    }
}
