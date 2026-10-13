package com.recipegrabber

import com.recipegrabber.data.local.dao.RecipeDao
import com.recipegrabber.data.remote.DriveBackupAuthorization
import com.recipegrabber.data.remote.DriveBackupTransport
import com.recipegrabber.domain.backup.DriveBackupCoordinator
import com.recipegrabber.domain.backup.RecipeBackupCodec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DriveBackupCoordinatorTest {
    private val dao = mockk<RecipeDao>(relaxed = true)
    private val auth = mockk<DriveBackupAuthorization>()
    private val transport = mockk<DriveBackupTransport>()
    private val coordinator = DriveBackupCoordinator(dao, auth, transport)

    @Test fun `invalid downloaded archive never reaches database`() = runTest {
        coEvery { auth.token() } returns "synthetic-token"
        coEvery { transport.download(any(), any()) } returns "broken".toByteArray()
        assertTrue(runCatching { coordinator.restore("fixture") }.isFailure)
        coVerify(exactly = 0) { dao.mergeBackup(any()) }
    }

    @Test fun `validated empty archive merges without deleting local recipes`() = runTest {
        coEvery { auth.token() } returns "synthetic-token"
        coEvery { transport.download(any(), any()) } returns RecipeBackupCodec.encode(emptyList())
        coEvery { dao.mergeBackup(emptyList()) } returns 0
        val result = coordinator.restore("fixture")
        assertEquals(0, result.inserted)
        assertEquals(0, result.skipped)
        coVerify(exactly = 1) { dao.mergeBackup(emptyList()) }
        coVerify(exactly = 0) { dao.deleteRecipe(any()) }
    }

    @Test fun `failed authorization prevents snapshot upload and cancellation propagates`() = runTest {
        coEvery { auth.token() } throws CancellationException("fixture")
        val failure = runCatching { coordinator.backup() }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        coVerify(exactly = 0) { dao.snapshotRecipes() }
        coVerify(exactly = 0) { transport.upload(any(), any(), any()) }
    }
}
