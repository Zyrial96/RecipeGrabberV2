package com.recipegrabber.di

import android.content.Context
import androidx.room.Room
import com.recipegrabber.data.local.MIGRATION_1_2
import com.recipegrabber.data.local.MIGRATION_2_3
import com.recipegrabber.data.local.RecipeDatabase
import com.recipegrabber.data.local.dao.IngredientDao
import com.recipegrabber.data.local.dao.RecipeDao
import com.recipegrabber.data.local.dao.StepDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRecipeDatabase(
        @ApplicationContext context: Context
    ): RecipeDatabase {
        return Room.databaseBuilder(
            context,
            RecipeDatabase::class.java,
            RecipeDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideRecipeDao(database: RecipeDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    @Singleton
    fun provideIngredientDao(database: RecipeDatabase): IngredientDao {
        return database.ingredientDao()
    }

    @Provides
    @Singleton
    fun provideStepDao(database: RecipeDatabase): StepDao {
        return database.stepDao()
    }
}
