package com.recipegrabber.data.remote

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
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
import com.google.api.services.drive.model.FileList
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
) {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private var driveService: Drive? = null

    private val credentialManager = CredentialManager.create(context)

    private val getCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(
            com.google.android.libraries.identity.googleid.GoogleIdTokenCredentialOption.Builder()
                .setServerClientId("YOUR_SERVER_CLIENT_ID")
                .setFilterByAuthorizedAccounts(true)
                .build()
        )
        .build()

    suspend fun signIn(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = credentialManager.getCredential(
                request = getCredentialRequest,
                context = context.mainExecutor,
                callback = object : androidx.credentials.CredentialManagerCallback<Credential, GetCredentialException> {
                    override fun onResult(result: Credential) {
                        handleCredentialResult(result)
                    }
                    override fun onError(e: GetCredentialException) {
                        // Handle error
                    }
                }
            )
            Result.success(result.credential)
        } catch (e: NoCredentialException) {
            tryGoogleSignInClient()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryGoogleSignInClient(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            val task = googleSignInClient.signInIntentSender
            // Handle the intent result in Activity
            Result.failure(Exception("Need activity context for sign-in"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleCredentialResult(credential: Credential) {
        when (credential) {
            is CustomCredential -> {
                if (credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                        .createFrom(credential.data)
                    _userEmail.value = googleIdTokenCredential.email
                    _isSignedIn.value = true
                    initializeDriveService(googleIdTokenCredential.email)
                }
            }
        }
    }

    fun initializeWithAccount(email: String) {
        _userEmail.value = email
        _isSignedIn.value = true
    }

    private fun initializeDriveService(email: String) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = android.accounts.Account(email, "com.google")

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

    fun signOut(): Result<Unit> {
        return try {
            _isSignedIn.value = false
            _userEmail.value = null
            driveService = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadRecipe(recipeJson: String, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in"))

            val folderResult = getOrCreateAppFolder()
            val folderId = folderResult.getOrNull() ?: return@withContext folderResult.cast()

            val metadata = File().apply {
                name = fileName
                mimeType = "application/json"
                parents = listOf(folderId)
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

            val folderResult = getOrCreateAppFolder()
            val folderId = folderResult.getOrNull() ?: return@withContext folderResult.cast()

            val result = drive.files().list()
                .setQ("'${folderId}' in parents and mimeType = 'application/json'")
                .setSpaces("drive")
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

    private suspend fun getOrCreateAppFolder(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in"))

            val result = drive.files().list()
                .setQ("name = 'Recipe Grabber' and mimeType = 'application/vnd.google-apps.folder' and 'appDataFolder' in parents")
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()

            val existingFolder = result.files?.firstOrNull()
            if (existingFolder != null) {
                return@withContext Result.success(existingFolder.id!!)
            }

            val folderMetadata = File().apply {
                name = "Recipe Grabber"
                mimeType = "application/vnd.google-apps.folder"
            }

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            Result.success(folder.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
