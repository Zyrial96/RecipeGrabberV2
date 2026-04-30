package com.recipegrabber.domain.llm

import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val googleDriveService: GoogleDriveService,
    private val logger: AppLogger
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            logger.i("Gemini", "Starting extraction for URL: $videoUrl")
            val modelId = preferencesRepository.llmModel.first()
            val model = if (modelId.isNotBlank() && modelId.startsWith("gemini")) {
                modelId
            } else {
                "gemini-2.5-flash"
            }
            logger.i("Gemini", "Using model: $model, URL: https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")

            val prompt = """Du bist ein Rezept-Extraktor. Analysiere das Video unter dieser URL und extrahiere NUR das Rezept, das im Video tatsächlich zubereitet wird. Erfinde keine Rezepte!

URL: $videoUrl

WICHTIGE REGELN:
- Antworte AUSSCHLIESSLICH auf Deutsch
- Verwende deutsche Maßeinheiten (g, kg, ml, l, EL, TL, Prise, Pck statt cups, oz, lbs, tbsp, tsp)
- Temperaturen in °C statt °F
- Extrahiere genau das EINE Rezept aus dem Video, nicht mehrere
- Gib alle Zutatenmengen exakt an
- Gib jeden Zubereitungsschritt vollständig und in der richtigen Reihenfolge an

Gib ein JSON-Objekt mit folgender Struktur zurück:
{
  "title": "Rezeptname auf Deutsch",
  "description": "Kurze Beschreibung auf Deutsch, was das Rezept ist",
  "servings": 4,
  "prepTimeMinutes": 15,
  "cookTimeMinutes": 30,
  "ingredients": [
    {"name": "Zutat auf Deutsch", "amount": 200, "unit": "g", "notes": "optional"}
  ],
  "steps": [
    {"order": 1, "instruction": "Schrittbeschreibung auf Deutsch", "duration": null}
  ],
  "sourceUrl": "$videoUrl",
  "sourceType": "VIDEO"
}""".trimMargin()

            val jsonBody = """
                {
                    "contents": [{
                        "parts": [{"text": ${gson.toJson(prompt)}}]
                    }],
                    "generationConfig": {
                        "responseMimeType": "application/json"
                    }
                }
            """.trimIndent()

            val requestAuth = when (preferencesRepository.geminiAuthMode.first()) {
                GeminiAuthMode.API_KEY -> {
                    val apiKey = preferencesRepository.geminiApiKey.first()
                    if (apiKey.isBlank()) {
                        logger.e("Gemini", "API key not configured")
                        return@withContext Result.failure(Exception("Gemini API key not configured"))
                    }
                    GeminiRequestAuth.ApiKey(apiKey)
                }
                GeminiAuthMode.GOOGLE_OAUTH -> {
                    val token = googleDriveService.getGenerativeLanguageAccessToken()
                        .getOrElse { error ->
                            return@withContext Result.failure(error)
                        }
                    GeminiRequestAuth.OAuth(token)
                }
            }

            val request = GeminiRequestFactory.buildGenerateContentRequest(model, jsonBody, requestAuth)

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                logger.e("Gemini", "API error ${response.code}: $errorBody")
                return@withContext Result.failure(Exception("Gemini API error: ${response.code} - $errorBody"))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("No response from Gemini"))
            logger.d("Gemini", "Response length: ${responseBody.length} chars")

            val parsed = parseGeminiResponse(responseBody, videoUrl)
            logger.i("Gemini", "Successfully extracted recipe: ${parsed.title}")
            Result.success(parsed)
        } catch (e: Exception) {
            logger.e("Gemini", "Extraction failed", e)
            Result.failure(e)
        }
    }

    private fun parseGeminiResponse(response: String, sourceUrl: String): Recipe {
        return try {
            val gson = com.google.gson.Gson()
            val geminiResponse = gson.fromJson(response, GeminiResponse::class.java)
            val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = text.replace("```json", "").replace("```", "").trim()

            val extracted = gson.fromJson(cleanJson, ExtractedRecipe::class.java)

            Recipe(
                id = 0,
                title = extracted.title ?: "Untitled Recipe",
                description = extracted.description ?: "",
                servings = extracted.servings ?: 4,
                prepTimeMinutes = extracted.prepTimeMinutes ?: 0,
                cookTimeMinutes = extracted.cookTimeMinutes ?: 0,
                sourceUrl = sourceUrl,
                sourceType = extracted.sourceType ?: "VIDEO",
                thumbnailUrl = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isFavorite = false,
                isSynced = false
            ).also { recipe ->
                recipe.ingredients = extracted.ingredients?.mapIndexed { index, ing ->
                    Ingredient(
                        id = index.toLong(),
                        recipeId = 0,
                        name = ing?.name ?: "",
                        amount = ing?.amount,
                        unit = ing?.unit ?: "",
                        notes = ing?.notes ?: "",
                        orderIndex = index
                    )
                } ?: emptyList()
                recipe.steps = extracted.steps?.mapIndexed { index, step ->
                    Step(
                        id = index.toLong(),
                        recipeId = 0,
                        order = step?.order ?: (index + 1),
                        instruction = step?.instruction ?: "",
                        duration = step?.duration,
                        imageUrl = null
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            Recipe(
                id = 0,
                title = "Extracted Recipe",
                description = "Recipe extracted from video",
                servings = 4,
                prepTimeMinutes = 0,
                cookTimeMinutes = 0,
                sourceUrl = sourceUrl,
                sourceType = "VIDEO",
                thumbnailUrl = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isFavorite = false,
                isSynced = false
            )
        }
    }

    companion object {
        private val gson = com.google.gson.Gson()
    }
}

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String?
)
