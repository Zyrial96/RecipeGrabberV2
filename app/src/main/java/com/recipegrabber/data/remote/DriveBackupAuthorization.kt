package com.recipegrabber.data.remote

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.recipegrabber.data.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupAuthorization @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: PreferencesRepository
) {
    suspend fun start(): GoogleAiAuthorizationStart {
        val result = request()
        if (result.hasResolution()) return GoogleAiAuthorizationStart.NeedsUserConsent(
            result.pendingIntent ?: error("Drive consent unavailable"))
        consume(result)
        return GoogleAiAuthorizationStart.Authorized(result.toGoogleSignInAccount()?.email)
    }

    suspend fun finish(data: Intent?) {
        consume(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data))
    }

    suspend fun token(): String {
        check(preferences.driveBackupConnected.first()) { "Connect Google Drive first" }
        val result = request()
        check(!result.hasResolution()) { "Google Drive needs renewed consent" }
        return consume(result)
    }

    private suspend fun request(): AuthorizationResult {
        val builder = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
        preferences.driveBackupAccount.first().takeIf { it.isNotBlank() }?.let { builder.setAccount(Account(it, "com.google")) }
        return Identity.getAuthorizationClient(context).authorize(builder.build()).await()
    }

    private suspend fun consume(result: AuthorizationResult): String {
        check(result.grantedScopes.orEmpty().contains(DriveScopes.DRIVE_APPDATA)) { "Drive permission not granted" }
        val token = result.accessToken?.takeIf { it.isNotBlank() } ?: error("Drive token unavailable")
        val previousAccount = preferences.driveBackupAccount.first()
        val returnedAccount = result.toGoogleSignInAccount()?.email?.takeIf { it.isNotBlank() }
        check(previousAccount.isBlank() || returnedAccount == null || returnedAccount == previousAccount) {
            "Drive account changed; disconnect before switching accounts"
        }
        val account = returnedAccount ?: previousAccount
        preferences.setDriveBackupConnection(true, account)
        return token
    }
}
