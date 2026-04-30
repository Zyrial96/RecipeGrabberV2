package com.recipegrabber.data.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import com.recipegrabber.data.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger
) {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private var driveService: Drive? = null
    private var googleSignInClient: GoogleSignInClient? = null

    init {
        googleSignInClient = buildSignInClient(includeGenerativeLanguage = false)

        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            _isSignedIn.value = true
            _userEmail.value = account.email
            initializeDriveService(account)
        }
    }

    fun getSignInIntent(includeGenerativeLanguage: Boolean = false): Intent {
        return buildSignInClient(includeGenerativeLanguage).signInIntent
    }

    suspend fun authorizeGenerativeLanguage(): Result<GoogleAiAuthorizationStart> = withContext(Dispatchers.IO) {
        try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(GoogleAiAuthorizationRequestFactory.buildGenerativeLanguageRequest())
                .await()

            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                    ?: return@withContext Result.failure(Exception("Google OAuth consent intent unavailable"))
                logger.i("GoogleOAuth", "User consent is required for Generative Language OAuth")
                Result.success(GoogleAiAuthorizationStart.NeedsUserConsent(pendingIntent))
            } else {
                logger.i("GoogleOAuth", "Generative Language OAuth was already authorized")
                consumeGenerativeLanguageAuthorizationResult(result)
                    .map { GoogleAiAuthorizationStart.Authorized(it.email) }
            }
        } catch (e: ApiException) {
            logger.e("GoogleOAuth", "Failed to start authorization with status: ${e.statusCode}", e)
            Result.failure(googleOAuthFailure("Google OAuth authorization failed", e))
        } catch (e: Exception) {
            logger.e("GoogleOAuth", "Failed to start authorization", e)
            Result.failure(e)
        }
    }

    fun handleGenerativeLanguageAuthorizationResult(data: Intent?): Result<GoogleAiAuthorization> {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            logger.i("GoogleOAuth", "Authorization result returned scopes: ${result.grantedScopes.orEmpty()}")
            consumeGenerativeLanguageAuthorizationResult(result)
        } catch (e: ApiException) {
            logger.e("GoogleOAuth", "Authorization result failed with status: ${e.statusCode}", e)
            Result.failure(googleOAuthFailure("Google OAuth authorization failed", e))
        } catch (e: Exception) {
            logger.e("GoogleOAuth", "Authorization result failed", e)
            Result.failure(e)
        }
    }

    fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            logger.i("Drive", "Sign-in successful for: ${account?.email}")
            
            account?.email?.let { email ->
                _isSignedIn.value = true
                _userEmail.value = email
                initializeDriveService(account)
                Result.success(email)
            } ?: Result.failure(Exception("No email returned"))
        } catch (e: ApiException) {
            logger.e("Drive", "Sign-in failed with status: ${e.statusCode}", e)
            Result.failure(Exception("Sign-in failed: ${e.statusCode}"))
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = googleSignInClient ?: return@withContext Result.failure(Exception("Not initialized"))
            logger.i("Drive", "Signing out")
            client.signOut().await()
            _isSignedIn.value = false
            _userEmail.value = null
            driveService = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun initializeDriveService(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Recipe Grabber V2")
                .build()
        } catch (e: Exception) {
            _isSignedIn.value = false
        }
    }

    suspend fun getGenerativeLanguageAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(GoogleAiAuthorizationRequestFactory.buildGenerativeLanguageRequest())
                .await()

            if (result.hasResolution()) {
                return@withContext Result.failure(Exception("Google AI OAuth permission required"))
            }

            consumeGenerativeLanguageAuthorizationResult(result).map { it.accessToken }
        } catch (e: ApiException) {
            logger.e("GoogleOAuth", "Failed to get Generative Language access token with status: ${e.statusCode}", e)
            Result.failure(googleOAuthFailure("Google OAuth access token failed", e))
        } catch (e: Exception) {
            logger.e("GoogleOAuth", "Failed to get Generative Language access token", e)
            Result.failure(e)
        }
    }

    private fun buildSignInClient(includeGenerativeLanguage: Boolean): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))

        if (includeGenerativeLanguage) {
            builder.requestScopes(Scope(GENERATIVE_LANGUAGE_SCOPE))
        }

        return GoogleSignIn.getClient(context, builder.build())
    }

    suspend fun uploadRecipe(recipeJson: String, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in"))

            val metadata = File().apply {
                name = fileName
                mimeType = "application/json"
                parents = listOf("appDataFolder")
            }

            val mediaContent = com.google.api.client.http.ByteArrayContent.fromString(
                "application/json",
                recipeJson
            )

            val file = drive.files().create(metadata, mediaContent)
                .setFields("id")
                .execute()

            Result.success(file.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadRecipes(): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in"))

            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("mimeType = 'application/json'")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            Result.success(result.files ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in"))
            drive.files().delete(fileId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val GENERATIVE_LANGUAGE_SCOPE = "https://www.googleapis.com/auth/generative-language.retriever"
    }
}

sealed class GoogleAiAuthorizationStart {
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : GoogleAiAuthorizationStart()
    data class Authorized(val email: String?) : GoogleAiAuthorizationStart()
}

data class GoogleAiAuthorization(
    val accessToken: String,
    val email: String?
)

private fun consumeGenerativeLanguageAuthorizationResult(
    result: AuthorizationResult
): Result<GoogleAiAuthorization> {
    val grantedScopes = result.grantedScopes.orEmpty()
    if (GoogleDriveService.GENERATIVE_LANGUAGE_SCOPE !in grantedScopes) {
        return Result.failure(Exception("Google AI OAuth scope was not granted"))
    }

    val token = result.accessToken
    if (token.isNullOrBlank()) {
        return Result.failure(Exception("Google OAuth access token unavailable"))
    }

    return Result.success(
        GoogleAiAuthorization(
            accessToken = token,
            email = result.toGoogleSignInAccount()?.email
        )
    )
}

private fun googleOAuthFailure(action: String, error: ApiException): Exception {
    val detail = error.message.orEmpty()
    val hint = if (detail.contains("UNREGISTERED_ON_API_CONSOLE")) {
        "Android OAuth client is not registered in Google API Console for com.recipegrabber and this signing certificate"
    } else {
        "status ${error.statusCode}"
    }
    return Exception("$action: $hint", error)
}
