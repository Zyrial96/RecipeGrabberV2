package com.recipegrabber.domain.llm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val googleDriveService: GoogleDriveService,
    private val logger: AppLogger,
    @ApplicationContext private val context: Context
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            logger.i("Gemini", "Starting video extraction")
            val modelId = preferencesRepository.llmModel.first()
            val model = if (modelId.isNotBlank() && modelId.startsWith("gemini")) {
                modelId
            } else {
                LlmModels.getDefaultModelForProvider(ProviderType.GEMINI).id
            }
            logger.i("Gemini", "Model selected")

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

            GeminiRequestFactory.buildVideoBody(videoUrl, prompt)

            var requestAuth = when (preferencesRepository.geminiAuthMode.first()) {
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

            GeminiFileUploader.create(client, context.cacheDir).withVideoBody(videoUrl, prompt, requestAuth,
                filesAuthorization = {
                    if (requestAuth is GeminiRequestAuth.OAuth) {
                        requestAuth = GeminiRequestAuth.OAuth(
                            googleDriveService.getGenerativeLanguageAccessToken(includeFiles = true).getOrThrow()
                        )
                    }
                    requestAuth
                }, onFilesReady = { logger.i("Gemini", "Video uploaded to Gemini Files and ACTIVE") }) { preparedBody ->
                val request = GeminiRequestFactory.buildGenerateContentRequest(model, preparedBody, requestAuth)
                client.newCall(request).consumeGeminiResponse { response ->
                    if (!response.isSuccessful) {
                        logger.e("Gemini", "API error ${response.code}")
                        return@consumeGeminiResponse Result.failure(geminiHttpFailure(response.code))
                    }
                    val responseBody = response.body?.string()
                        ?: return@consumeGeminiResponse Result.failure(Exception("No response from Gemini"))
                    val parsed = GeminiResponseParser.parse(responseBody, videoUrl)
                    logger.i("Gemini", "Recipe extraction succeeded")
                    Result.success(parsed)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("Gemini", "Extraction failed", e)
            Result.failure(e)
        }
    }

}

internal fun geminiHttpFailure(code: Int): Exception {
    val message = when (code) {
        401 -> "Gemini-Anmeldung fehlgeschlagen. Google-Verbindung oder API-Schluessel pruefen."
        403 -> "Gemini-Zugriff verweigert. Cloud-Projekt, Berechtigungen und Modellzugriff pruefen."
        429 -> "Gemini-Anfragelimit erreicht. Kontingent pruefen und spaeter erneut versuchen."
        500, 502, 503, 504 -> "Gemini ist voruebergehend nicht verfuegbar. Bitte spaeter erneut versuchen."
        else -> "Gemini konnte die Anfrage nicht verarbeiten."
    }
    return Exception("$message (HTTP $code)")
}
