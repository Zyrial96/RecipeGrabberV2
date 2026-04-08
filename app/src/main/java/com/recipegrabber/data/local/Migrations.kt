package com.recipegrabber.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipes_backup` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `servings` INTEGER NOT NULL DEFAULT 4,
                `prepTimeMinutes` INTEGER NOT NULL DEFAULT 0,
                `cookTimeMinutes` INTEGER NOT NULL DEFAULT 0,
                `sourceUrl` TEXT NOT NULL DEFAULT '',
                `sourceType` TEXT NOT NULL DEFAULT 'VIDEO',
                `thumbnailUrl` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            INSERT INTO recipes_backup 
            SELECT id, title, description, servings, prepTimeMinutes, cookTimeMinutes, 
                   sourceUrl, sourceType, thumbnailUrl, createdAt, updatedAt 
            FROM recipes
        """.trimIndent())

        database.execSQL("DROP TABLE recipes")
        database.execSQL("ALTER TABLE recipes_backup RENAME TO recipes")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `ingredients_backup` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `recipeId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL,
                `unit` TEXT NOT NULL DEFAULT '',
                `notes` TEXT NOT NULL DEFAULT '',
                `orderIndex` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("""
            INSERT INTO ingredients_backup 
            SELECT id, recipeId, name, amount, unit, notes, orderIndex 
            FROM ingredients
        """.trimIndent())

        database.execSQL("DROP TABLE ingredients")
        database.execSQL("ALTER TABLE ingredients_backup RENAME TO ingredients")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId` ON `ingredients` (`recipeId`)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `steps_backup` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `recipeId` INTEGER NOT NULL,
                `order` INTEGER NOT NULL,
                `instruction` TEXT NOT NULL,
                `duration` INTEGER,
                `imageUrl` TEXT,
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("""
            INSERT INTO steps_backup 
            SELECT id, recipeId, `order`, instruction, duration, imageUrl 
            FROM steps
        """.trimIndent())

        database.execSQL("DROP TABLE steps")
        database.execSQL("ALTER TABLE steps_backup RENAME TO steps")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_recipeId` ON `steps` (`recipeId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE recipes ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
    }
}
