package com.recipegrabber.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.recipegrabber.data.local.dao.IngredientDao
import com.recipegrabber.data.local.dao.RecipeDao
import com.recipegrabber.data.local.dao.StepDao
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step

@Database(
    entities = [Recipe::class, Ingredient::class, Step::class],
    version = 3,
    exportSchema = true
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun stepDao(): StepDao

    companion object {
        const val DATABASE_NAME = "recipe_grabber_db"
    }
}
