package com.recipegrabber.domain.llm

import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KimiProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val logger: AppLogger
) : LlmProvider {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.moonshot.ai/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(KimiApi::class.java)

    override suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            logger.i("Kimi", "Starting extraction for URL: $videoUrl")
            val apiKey = preferencesRepository.kimiApiKey.first()
            if (apiKey.isBlank()) {
                logger.e("Kimi", "API key not configured")
                return@withContext Result.failure(Exception("Kimi API key not configured"))
            }

            val modelId = preferencesRepository.llmModel.first()
            val model = if (modelId.isNotBlank() && modelId.startsWith("moonshot")) {
                modelId
            } else {
                "moonshot-v1-128k"
            }
            logger.i("Kimi", "Using model: $model")

            val response = api.extractRecipe(
                authorization = "Bearer $apiKey",
                request = KimiRequest(
                    model = model,
                    messages = listOf(
                        KimiMessage(
                            role = "user",
                            content = """Extract the recipe from this video URL: $videoUrl
                            Return a JSON object with:
                            {
                              "title": "Recipe Name",
                              "description": "Brief description",
                              "servings": 4,
                              "prepTimeMinutes": 15,
                              "cookTimeMinutes": 30,
                              "ingredients": [
                                {"name": "ingredient", "amount": 1.0, "unit": "cup", "notes": ""}
                              ],
                              "steps": [
                                {"order": 1, "instruction": "Step text", "duration": null}
                              ],
                              "sourceUrl": "$videoUrl",
                              "sourceType": "VIDEO"
                            }"""
                        )
                    )
                )
            )

val content = response.choices.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    logger.e("Kimi", "Empty response from API")
                    return@withContext Result.failure(Exception("No response from Kimi"))
                }
                logger.d("Kimi", "Raw response: ${content.take(500)}")

            val recipe = parseKimiResponse(content, videoUrl)
            logger.i("Kimi", "Successfully extracted recipe: ${recipe.title}")
            Result.success(recipe)
        } catch (e: Exception) {
            logger.e("Kimi", "Extraction failed", e)
            Result.failure(e)
        }
    }

    private fun parseKimiResponse(json: String, sourceUrl: String): Recipe {
        return try {
            val gson = com.google.gson.Gson()
            val extracted = gson.fromJson(json.trim(), ExtractedRecipe::class.java)
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
                description = "Recipe from video",
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
}

interface KimiApi {
    @POST("v1/chat/completions")
    suspend fun extractRecipe(
        @Header("Authorization") authorization: String,
        @Body request: KimiRequest
    ): KimiResponse
}

data class KimiRequest(
    val model: String,
    val messages: List<KimiMessage>
)

data class KimiMessage(
    val role: String,
    val content: String
)

data class KimiResponse(
    val choices: List<KimiChoice>
)

data class KimiChoice(
    val message: KimiResponseMessage
)

data class KimiResponseMessage(
    val content: String
)