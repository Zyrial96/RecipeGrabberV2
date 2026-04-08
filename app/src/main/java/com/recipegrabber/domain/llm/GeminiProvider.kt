package com.recipegrabber.domain.llm

import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferencesRepository.geminiApiKey.first()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Gemini API key not configured"))
            }

            val modelId = preferencesRepository.llmModel.first()
            val model = if (modelId.isNotBlank() && modelId.startsWith("gemini")) {
                modelId
            } else {
                "gemini-2.0-flash"
            }

            val prompt = """Extract the recipe from this video: $videoUrl
                |Return a JSON object with the following structure:
                |{
                |  "title": "Recipe Name",
                |  "description": "Brief description",
                |  "servings": 4,
                |  "prepTimeMinutes": 15,
                |  "cookTimeMinutes": 30,
                |  "ingredients": [
                |    {"name": "ingredient name", "amount": 1.0, "unit": "cup", "notes": "optional notes"}
                |  ],
                |  "steps": [
                |    {"order": 1, "instruction": "Step instruction", "duration": null}
                |  ],
                |  "sourceUrl": "$videoUrl",
                |  "sourceType": "VIDEO"
                |}""".trimMargin()

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

            val mediaType = "application/json".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("No response from Gemini"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API error: ${response.code}"))
            }

            val parsed = parseGeminiResponse(responseBody, videoUrl)
            Result.success(parsed)
        } catch (e: Exception) {
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