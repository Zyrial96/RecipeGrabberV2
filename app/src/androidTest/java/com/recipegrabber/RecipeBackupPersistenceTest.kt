package com.recipegrabber

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.recipegrabber.data.local.MIGRATION_3_4
import com.recipegrabber.data.local.RecipeDatabase
import com.recipegrabber.data.local.entity.*
import com.recipegrabber.domain.backup.RecipeBackupCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RecipeBackupPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun entry(title: String, instruction: String = "Kochen") = RecipeWithDetails(
        Recipe(title = title, servings = 2, sourceUrl = "https://example.org/recipe.webm"),
        listOf(Ingredient(recipeId = 0, name = "Wasser", amount = 250.5, unit = "ml")),
        listOf(Step(recipeId = 0, order = 1, instruction = instruction))
    )

    @Test fun restorePreservesDistinctDuplicatesAndRepeatingItDoesNotDuplicateRecipes() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        try {
            val dao = database.recipeDao()
            val original = listOf(entry("QA Suppe"), entry("QA Suppe"))
            val restored = RecipeBackupCodec.decode(RecipeBackupCodec.encode(original))
            assertEquals(2, dao.mergeBackup(restored))
            assertEquals(0, dao.mergeBackup(restored))
            val saved = dao.snapshotRecipes()
            assertEquals(2, saved.size)
            assertEquals(2, saved.map { it.recipe.backupId }.distinct().size)
            assertTrue(saved.all { it.ingredients.single().recipeId == it.recipe.id && it.steps.single().recipeId == it.recipe.id })
        } finally { database.close() }
    }

    @Test fun localEditsWinAgainstOlderSnapshots() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        try {
            val dao = database.recipeDao()
            val original = entry("QA Suppe")
            val id = dao.insertRecipeWithDetails(original.recipe.copy(title = "Local edit", isFavorite = true), original.ingredients, original.steps)
            assertEquals(0, dao.mergeBackup(listOf(original)))
            val saved = dao.snapshotRecipes().single()
            assertEquals(id, saved.recipe.id)
            assertEquals("Local edit", saved.recipe.title)
            assertTrue(saved.recipe.isFavorite)
        } finally { database.close() }
    }

    @Test fun identityCollisionCannotReplaceAnExistingAggregate() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        try {
            val dao = database.recipeDao()
            val original = entry("Existing")
            dao.insertRecipeWithDetails(original.recipe, original.ingredients, original.steps)
            try {
                dao.insertRecipeWithDetails(original.recipe.copy(title = "Attempted overwrite"), original.ingredients, original.steps)
                fail("Identity collision must abort")
            } catch (_: android.database.sqlite.SQLiteException) { }
            val saved = dao.snapshotRecipes().single()
            assertEquals("Existing", saved.recipe.title)
            assertEquals(1, saved.ingredients.size)
            assertEquals(1, saved.steps.size)
        } finally { database.close() }
    }

    @Test fun oneFailedAggregateRollsBackTheWholeMerge() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        try {
            val dao = database.recipeDao()
            val existing = entry("Existing")
            dao.insertRecipeWithDetails(existing.recipe, existing.ingredients, existing.steps)
            database.openHelper.writableDatabase.execSQL("CREATE TRIGGER qa_reject BEFORE INSERT ON steps WHEN NEW.instruction = 'Reject' BEGIN SELECT RAISE(ABORT, 'Synthetic failure'); END")
            try { dao.mergeBackup(listOf(entry("First"), entry("Second", "Reject"))); fail("Merge should fail") }
            catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(listOf("Existing"), dao.snapshotRecipes().map { it.recipe.title })
        } finally { database.close() }
    }

    @Test fun versionThreeMigrationPreservesRecipesChildrenAndAddsStableIds() = runBlocking {
        val name = "backup-migration-qa-${UUID.randomUUID()}"
        val asset = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.recipegrabber.data.local.RecipeDatabase/3.json").bufferedReader().use { it.readText() }
        val schema = JsonParser.parseString(asset).asJsonObject.getAsJsonObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    schema.getAsJsonArray("entities").forEach { value ->
                        val entity = value.asJsonObject
                        val table = entity["tableName"].asString
                        db.execSQL(entity["createSql"].asString.replace("\${TABLE_NAME}", table))
                        entity.getAsJsonArray("indices").forEach { index -> db.execSQL(index.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}", table)) }
                    }
                    schema.getAsJsonArray("setupQueries").forEach { db.execSQL(it.asString) }
                    db.execSQL("INSERT INTO recipes VALUES (7, 'QA legacy', '', 2, 0, 0, 'https://example.org/recipe.webm', 'VIDEO', NULL, 100, 200, 1, 0)")
                    db.execSQL("INSERT INTO ingredients VALUES (11, 7, 'Wasser', 250.5, 'ml', '', 0)")
                    db.execSQL("INSERT INTO steps VALUES (18, 7, 1, 'Kochen', 60, NULL)")
                    db.execSQL("INSERT INTO recipes VALUES (8, 'QA legacy', '', 2, 0, 0, 'https://example.org/recipe.webm', 'VIDEO', NULL, 100, 200, 0, 0)")
                    db.execSQL("INSERT INTO ingredients VALUES (12, 8, 'Salz', 2.5, 'g', '', 0)")
                    db.execSQL("INSERT INTO steps VALUES (19, 8, 1, 'Salzen', NULL, NULL)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        try {
            helper.writableDatabase
            helper.close()
            val migrated = Room.databaseBuilder(context, RecipeDatabase::class.java, name).addMigrations(MIGRATION_3_4).build()
            try {
                val all = migrated.recipeDao().snapshotRecipes()
                assertEquals(setOf(7L, 8L), all.map { it.recipe.id }.toSet())
                assertEquals(2, all.map { it.recipe.backupId }.distinct().size)
                val saved = all.single { it.recipe.id == 7L }
                assertEquals(7L, saved.recipe.id)
                assertEquals("QA legacy", saved.recipe.title)
                assertTrue(saved.recipe.isFavorite)
                assertEquals(250.5, saved.ingredients.single().amount)
                assertEquals(18L, saved.steps.single().id)
                assertEquals(saved.recipe.backupId, UUID.fromString(saved.recipe.backupId).toString())
                assertEquals(2, RecipeBackupCodec.decode(RecipeBackupCodec.encode(all)).size)
            } finally { migrated.close() }
        } finally { helper.close(); context.deleteDatabase(name) }
    }
}
