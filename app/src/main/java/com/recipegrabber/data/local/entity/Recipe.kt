package com.recipegrabber.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.util.UUID

@Entity(tableName = "recipes", indices = [Index(value = ["backupId"], unique = true)])
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val servings: Int = 4,
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val sourceUrl: String = "",
    val sourceType: String = "VIDEO",
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "''")
    val backupId: String = UUID.randomUUID().toString()
) {
    @Transient
    var ingredients: List<Ingredient> = emptyList()
    @Transient
    var steps: List<Step> = emptyList()
}
