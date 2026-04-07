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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private var driveService: Drive? = null
    private var googleSignInClient: GoogleSignInClient? = null

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            _isSignedIn.value = true
            _userEmail.value = account.email
            initializeDriveService(account)
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient?.signInIntent 
            ?: throw IllegalStateException("GoogleSignInClient not initialized")
    }

    fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            account?.email?.let { email ->
                _isSignedIn.value = true
                _userEmail.value = email
                initializeDriveService(account)
                Result.success(email)
            } ?: Result.failure(Exception("No email returned"))
        } catch (e: ApiException) {
            Result.failure(Exception("Sign-in failed: ${e.statusCode}"))
        }
    }

    fun signOut(): Result<Unit> {
        return try {
            googleSignInClient?.signOut()?.addOnCompleteListener {
                _isSignedIn.value = false
                _userEmail.value = null
                driveService = null
            }
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
}