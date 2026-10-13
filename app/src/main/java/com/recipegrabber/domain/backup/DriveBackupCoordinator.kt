package com.recipegrabber.domain.backup

import com.recipegrabber.data.local.dao.RecipeDao
import com.recipegrabber.data.remote.DriveBackupAuthorization
import com.recipegrabber.data.remote.DriveBackupTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RestoreResult(val inserted: Int, val skipped: Int)

class DriveBackupCoordinator @Inject constructor(
    private val recipes: RecipeDao,
    private val authorization: DriveBackupAuthorization,
    private val transport: DriveBackupTransport
) {
    suspend fun list() = transport.list(authorization.token())

    suspend fun backup(): Int = withContext(Dispatchers.IO) {
        val token = authorization.token()
        val snapshot = recipes.snapshotRecipes()
        transport.upload(token, RecipeBackupCodec.encode(snapshot), snapshot.size)
        snapshot.size
    }

    suspend fun restore(id: String): RestoreResult = withContext(Dispatchers.IO) {
        val snapshot = RecipeBackupCodec.decode(transport.download(authorization.token(), id))
        val inserted = recipes.mergeBackup(snapshot)
        RestoreResult(inserted, snapshot.size - inserted)
    }
}
