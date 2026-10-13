package com.recipegrabber

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.recipegrabber.data.remote.DriveBackupAuthorization
import com.recipegrabber.data.remote.DriveBackupTransport
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.di.DatabaseModule
import com.recipegrabber.domain.backup.DriveBackupCoordinator
import com.recipegrabber.domain.backup.RecipeBackupCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class DriveBackupLiveProbeTest {
    @Test fun realCloudRoundTripPreservesExistingLibrary(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("driveLiveProbe") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName == "com.recipegrabber") { "Live OAuth probe is main-app only" }
        val database = DatabaseModule.provideRecipeDatabase(context)
        try {
            val dao = database.recipeDao()
            val backups = DriveBackupCoordinator(dao, DriveBackupAuthorization(context, PreferencesRepository(context)), DriveBackupTransport())
            val before = dao.snapshotRecipes().sortedBy { it.recipe.id }
            assertTrue("Existing recipes required for non-destructive acceptance", before.isNotEmpty())
            val expectedCount = InstrumentationRegistry.getArguments().getString("expectedRecipes")?.toInt()
            expectedCount?.let { assertEquals(it, before.size) }
            val original = RecipeBackupCodec.encode(before, createdAt = 0)
            val previous = backups.list().map { it.id }.toSet()
            assertEquals(before.size, backups.backup())
            val uploaded = backups.list().filter { it.id !in previous }
            assertEquals("One new snapshot must be visible", 1, uploaded.size)
            assertEquals(before.size, uploaded.single().recipeCount)
            repeat(2) {
                val result = backups.restore(uploaded.single().id)
                assertEquals(0, result.inserted)
                assertEquals(before.size, result.skipped)
                val after = dao.snapshotRecipes().sortedBy { it.recipe.id }
                assertEquals(before.map { it.recipe.id }, after.map { it.recipe.id })
                assertArrayEquals(original, RecipeBackupCodec.encode(after, createdAt = 0))
            }
            Log.i("RG-DriveProbe", "Cloud upload/list/download/restore passed; recipes=${before.size}; repeated restore inserted=0")
        } finally { database.close() }
    }
}
