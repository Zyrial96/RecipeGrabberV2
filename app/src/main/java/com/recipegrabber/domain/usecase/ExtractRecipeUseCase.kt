package com.recipegrabber.domain.usecase

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.remote.apify.ApifyService
import com.recipegrabber.data.remote.apify.ScrapedVideoData
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.LlmProvider
import com.recipegrabber.domain.llm.LlmProviderFactory
import com.recipegrabber.domain.llm.ProviderType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractRecipeUseCase @Inject constructor(
    private val apifyService: ApifyService,
    private val llmProviderFactory: LlmProviderFactory,
    private val preferencesRepository: PreferencesRepository,
    private val saveRecipeUseCase: SaveRecipeUseCase
) {

    sealed class ExtractionResult {
        data class Success(val recipe: Recipe) : ExtractionResult()
        data class Error(val message: String) : ExtractionResult()
        data class ScrapingFailed(val message: String) : ExtractionResult()
        data class NoApiKey(val provider: String) : ExtractionResult()
    }

    suspend operator fun invoke(videoUrl: String): ExtractionResult {
        // Check which platform
        val platform = detectPlatform(videoUrl)
        
        // Try Apify first if available
        val apifyKey = preferencesRepository.apifyApiKey.first()
        
        val scrapedData = if (apifyKey.isNotBlank() && (platform == Platform.TIKTOK || platform == Platform.INSTAGRAM)) {
            scrapeWithApify(videoUrl, apifyKey)
        } else {
            null
        }

        // Get LLM provider
        val providerType = preferencesRepository.llmProviderType.first()
        val provider = llmProviderFactory.create(providerType)

        // Check API key
        val hasApiKey = when (providerType) {
            ProviderType.OPENAI -> preferencesRepository.openAiApiKey.first().isNotBlank()
            ProviderType.GEMINI -> preferencesRepository.geminiApiKey.first().isNotBlank()
            ProviderType.CLAUDE -> preferencesRepository.claudeApiKey.first().isNotBlank()
            ProviderType.KIMI -> preferencesRepository.kimiApiKey.first().isNotBlank()
        }

        if (!hasApiKey) {
            return ExtractionResult.NoApiKey(providerType.name)
        }

        return try {
            // If we have scraped data, pass it to LLM with more context
            val contextUrl = scrapedData?.videoUrl ?: videoUrl
            val description = scrapedData?.description ?: ""

            val result = provider.extractRecipeFromVideo(contextUrl)
            
            result.fold(
                onSuccess = { extractedRecipe ->
                    // Enhance with scraped description if available
                    val enhancedRecipe = if (description.isNotBlank() && extractedRecipe.description.isBlank()) {
                        extractedRecipe.copy(description = description)
                    } else {
                        extractedRecipe
                    }
                    
                    // Save to database
                    val savedId = saveRecipeUseCase(enhancedRecipe)
                    ExtractionResult.Success(enhancedRecipe.copy(id = savedId))
                },
                onFailure = { error ->
                    ExtractionResult.Error(error.message ?: "Extraction failed")
                }
            )
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun scrapeWithApify(url: String, apiKey: String): ScrapedVideoData? {
        return try {
            val result = when (detectPlatform(url)) {
                Platform.TIKTOK -> apifyService.scrapeTikTokVideo(url, apiKey)
                Platform.INSTAGRAM -> apifyService.scrapeInstagramReel(url, apiKey)
                else -> return null
            }
            
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun detectPlatform(url: String): Platform {
        return when {
            url.contains("tiktok.com", ignoreCase = true) -> Platform.TIKTOK
            url.contains("instagram.com", ignoreCase = true) -> Platform.INSTAGRAM
            url.contains("youtube.com", ignoreCase = true) || 
            url.contains("youtu.be", ignoreCase = true) -> Platform.YOUTUBE
            url.contains("facebook.com", ignoreCase = true) -> Platform.FACEBOOK
            url.contains("twitter.com", ignoreCase = true) || 
            url.contains("x.com", ignoreCase = true) -> Platform.TWITTER
            else -> Platform.UNKNOWN
        }
    }

    private enum class Platform {
        TIKTOK, INSTAGRAM, YOUTUBE, FACEBOOK, TWITTER, UNKNOWN
    }
}