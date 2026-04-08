package com.recipegrabber.domain.llm

import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
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
class ClaudeProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : LlmProvider {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.anthropic.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ClaudeApi::class.java)

    override suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferencesRepository.claudeApiKey.first()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Claude API key not configured"))
            }

            val modelId = preferencesRepository.llmModel.first()
            val model = if (modelId.isNotBlank() && modelId.startsWith("claude")) {
                modelId
            } else {
                "claude-3-5-sonnet-20241022"
            }

            val response = api.extractRecipe(
                apiKey = apiKey,
                anthropicVersion = "2023-06-01",
                request = ClaudeRequest(
                    model = model,
                    max_tokens = 4096,
                    messages = listOf(
                        ClaudeMessage(
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

            val content = response.content.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No response from Claude"))

            Result.success(parseClaudeResponse(content, videoUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseClaudeResponse(json: String, sourceUrl: String): Recipe {
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

interface ClaudeApi {
    @POST("v1/messages")
    suspend fun extractRecipe(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String,
        @Body request: ClaudeRequest
    ): ClaudeResponse
}

data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val content: List<ClaudeContent>
)

data class ClaudeContent(
    val type: String,
    val text: String
)