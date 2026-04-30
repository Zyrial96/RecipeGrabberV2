package com.recipegrabber.data.remote

import android.content.Context
import android.content.Intent
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
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Google OAuth sign-in required"))

            if (!GoogleSignIn.hasPermissions(account, Scope(GENERATIVE_LANGUAGE_SCOPE))) {
                return@withContext Result.failure(Exception("Google AI OAuth permission required"))
            }

            val selectedAccount = account.account
                ?: return@withContext Result.failure(Exception("Google account not available"))

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(GENERATIVE_LANGUAGE_SCOPE)
            )
            credential.selectedAccount = selectedAccount

            val token = credential.token
            if (token.isNullOrBlank()) {
                Result.failure(Exception("Google OAuth access token unavailable"))
            } else {
                Result.success(token)
            }
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
