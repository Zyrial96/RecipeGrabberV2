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
class OpenAiProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : LlmProvider {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenAiApi::class.java)

    override suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferencesRepository.openAiApiKey.first()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("OpenAI API key not configured"))
            }

            val modelId = preferencesRepository.llmModel.first()
            val model = if (modelId.isNotBlank()) modelId else LlmModels.GPT_4O.id

            val response = api.extractRecipe(
                authorization = "Bearer $apiKey",
                request = ExtractionRequest(
                    model = model,
                    messages = listOf(
                        Message(
                            role = "user",
                            content = """Extract the recipe from this video: $videoUrl
                            Return a JSON object with the following structure:
                            {
                              "title": "Recipe Name",
                              "description": "Brief description",
                              "servings": 4,
                              "prepTimeMinutes": 15,
                              "cookTimeMinutes": 30,
                              "ingredients": [
                                {"name": "ingredient name", "amount": 1.0, "unit": "cup", "notes": "optional notes"}
                              ],
                              "steps": [
                                {"order": 1, "instruction": "Step instruction", "duration": null}
                              ],
                              "sourceUrl": "$videoUrl",
                              "sourceType": "VIDEO"
                            }"""
                        )
                    )
                )
            )

            val content = response.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("No response from OpenAI"))

            val cleanJson = content.replace("```json", "").replace("```", "").trim()
            val recipe = parseRecipeFromJson(cleanJson, videoUrl)
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRecipeFromJson(json: String, sourceUrl: String): Recipe {
        val regex = """\{.*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(json)?.value ?: json

        return try {
            val gson = com.google.gson.Gson()
            val extracted = gson.fromJson(match, ExtractedRecipe::class.java)
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
}

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun extractRecipe(
        @Header("Authorization") authorization: String,
        @Body request: ExtractionRequest
    ): ChatCompletionResponse
}

data class ExtractionRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ResponseMessage
)

data class ResponseMessage(
    val content: String
)

data class ExtractedRecipe(
    val title: String?,
    val description: String?,
    val servings: Int?,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val ingredients: List<ExtractedIngredient>?,
    val steps: List<ExtractedStep>?,
    val sourceUrl: String?,
    val sourceType: String?
)

data class ExtractedIngredient(
    val name: String?,
    val amount: Double?,
    val unit: String?,
    val notes: String?
)

data class ExtractedStep(
    val order: Int?,
    val instruction: String?,
    val duration: Int?
)