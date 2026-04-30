package com.recipegrabber.data.remote.apify

import com.recipegrabber.data.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApifyService @Inject constructor(
    private val logger: AppLogger
) {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.apify.com/v2/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ApifyApi::class.java)

    suspend fun scrapeInstagramReel(url: String, apiKey: String): Result<ScrapedVideoData> = withContext(Dispatchers.IO) {
        try {
            logger.i("Apify", "Starting Instagram scrape for: $url")
            val runResponse = api.startActorRun(
                authorization = "Bearer $apiKey",
                actorId = "apify/instagram-scraper",
                request = ActorRunRequest(
                    startUrls = listOf(StartUrl(url = url)),
                    resultsType = "details"
                )
            )

            val runId = runResponse.data?.id
            if (runId == null) {
                logger.e("Apify", "Failed to start Instagram scraper - no run ID returned")
                return@withContext Result.failure(Exception("Failed to start Instagram scraper"))
            }
            logger.d("Apify", "Instagram scraper run started: $runId")

            var attempts = 0
            var datasetId: String? = null

            while (attempts < 30) {
                kotlinx.coroutines.delay(2000)
                val statusResponse = api.getRunStatus(
                    authorization = "Bearer $apiKey",
                    runId = runId
                )

                when (statusResponse.data?.status) {
                    "SUCCEEDED" -> {
                        datasetId = statusResponse.data.defaultDatasetId
                        break
                    }
                    "FAILED", "TIMED_OUT", "ABORTED" -> {
                        return@withContext Result.failure(Exception("Scraping failed: ${statusResponse.data.status}"))
                    }
                }
                attempts++
            }

            if (datasetId == null) {
                return@withContext Result.failure(Exception("Scraping timeout"))
            }

            val items = api.getDatasetItems(
                authorization = "Bearer $apiKey",
                datasetId = datasetId,
                limit = 1
            )

            val item = items.firstOrNull()
                ?: return@withContext Result.failure(Exception("No data returned"))

            Result.success(
                ScrapedVideoData(
                    videoUrl = item.videoUrl,
                    description = item.caption ?: "",
                    thumbnailUrl = item.displayUrl,
                    platform = "INSTAGRAM"
                )
            )
        } catch (e: Exception) {
            logger.e("Apify", "Instagram scrape failed", e)
            Result.failure(e)
        }
    }

    suspend fun scrapeTikTokVideo(url: String, apiKey: String): Result<ScrapedVideoData> = withContext(Dispatchers.IO) {
        try {
            logger.i("Apify", "Starting TikTok scrape for: $url")
            val runResponse = api.startActorRun(
                authorization = "Bearer $apiKey",
                actorId = "clockworks/tiktok-scraper",
                request = ActorRunRequest(
                    startUrls = listOf(StartUrl(url = url)),
                    resultsType = "details"
                )
            )

            val runId = runResponse.data?.id
            if (runId == null) {
                logger.e("Apify", "Failed to start TikTok scraper - no run ID returned")
                return@withContext Result.failure(Exception("Failed to start TikTok scraper"))
            }
            logger.d("Apify", "TikTok scraper run started: $runId")

            var attempts = 0
            var datasetId: String? = null

            while (attempts < 30) {
                kotlinx.coroutines.delay(2000)
                val statusResponse = api.getRunStatus(
                    authorization = "Bearer $apiKey",
                    runId = runId
                )

                when (statusResponse.data?.status) {
                    "SUCCEEDED" -> {
                        datasetId = statusResponse.data.defaultDatasetId
                        break
                    }
                    "FAILED", "TIMED_OUT", "ABORTED" -> {
                        return@withContext Result.failure(Exception("Scraping failed: ${statusResponse.data.status}"))
                    }
                }
                attempts++
            }

            if (datasetId == null) {
                return@withContext Result.failure(Exception("Scraping timeout"))
            }

            val items = api.getDatasetItems(
                authorization = "Bearer $apiKey",
                datasetId = datasetId,
                limit = 1
            )

            val item = items.firstOrNull()
                ?: return@withContext Result.failure(Exception("No data returned"))

            Result.success(
                ScrapedVideoData(
                    videoUrl = item.videoUrl,
                    description = item.text ?: "",
                    thumbnailUrl = item.cover,
                    platform = "TIKTOK"
                )
            )
        } catch (e: Exception) {
            logger.e("Apify", "TikTok scrape failed", e)
            Result.failure(e)
        }
    }
}

interface ApifyApi {
    @POST("acts/{actorId}/runs")
    suspend fun startActorRun(
        @Header("Authorization") authorization: String,
        @Path("actorId") actorId: String,
        @Body request: ActorRunRequest
    ): ActorRunResponse

    @GET("actor-runs/{runId}")
    suspend fun getRunStatus(
        @Header("Authorization") authorization: String,
        @Path("runId") runId: String
    ): RunStatusResponse

    @GET("datasets/{datasetId}/items")
    suspend fun getDatasetItems(
        @Header("Authorization") authorization: String,
        @Path("datasetId") datasetId: String,
        @Query("limit") limit: Int = 1
    ): List<DatasetItem>
}

data class ActorRunRequest(
    val startUrls: List<StartUrl>,
    val resultsType: String
)

data class StartUrl(
    val url: String
)

data class ActorRunResponse(
    val data: RunData?
)

data class RunData(
    val id: String,
    val status: String,
    val defaultDatasetId: String?
)

data class RunStatusResponse(
    val data: RunStatus?
)

data class RunStatus(
    val id: String,
    val status: String,
    val defaultDatasetId: String?
)

data class DatasetItem(
    val videoUrl: String?,
    val caption: String?,
    val displayUrl: String?,
    val text: String?,
    val cover: String?
)

data class ScrapedVideoData(
    val videoUrl: String?,
    val description: String,
    val thumbnailUrl: String?,
    val platform: String
)
