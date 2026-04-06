package com.recipegrabber.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recipegrabber.data.local.entity.Step
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY `order` ASC")
    fun getStepsForRecipe(recipeId: Long): Flow<List<Step>>

    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY `order` ASC")
    suspend fun getStepsForRecipeSync(recipeId: Long): List<Step>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: Step): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<Step>)

    @Update
    suspend fun updateStep(step: Step)

    @Delete
    suspend fun deleteStep(step: Step)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsForRecipe(recipeId: Long)
}
