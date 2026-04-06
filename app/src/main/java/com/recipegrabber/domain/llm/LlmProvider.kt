package com.recipegrabber.domain.llm

import com.recipegrabber.data.local.entity.Recipe

interface LlmProvider {
    suspend fun extractRecipeFromVideo(videoUrl: String): Result<Recipe>
}
