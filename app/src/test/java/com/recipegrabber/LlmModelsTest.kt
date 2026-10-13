package com.recipegrabber

import com.recipegrabber.domain.llm.LlmModels
import com.recipegrabber.domain.llm.ProviderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LlmModelsTest {
    @Test
    fun `catalog IDs are unique`() {
        val ids = LlmModels.ALL_MODELS.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `Gemini defaults to selectable lightweight video model`() {
        val model = LlmModels.getDefaultModelForProvider(ProviderType.GEMINI)
        assertEquals("gemini-3.1-flash-lite", model.id)
        assertTrue(model.isVisionCapable)
        assertEquals(1048576, model.contextWindow)
    }

    @Test
    fun `lightweight Gemini video model is explicitly selectable`() {
        val model = LlmModels.getModelById("gemini-3.1-flash-lite")
        assertNotNull(model)
        assertEquals(ProviderType.GEMINI, model?.provider)
        assertTrue(model?.isVisionCapable == true)
        assertTrue(LlmModels.getModelsForProvider(ProviderType.GEMINI).contains(model))
        assertEquals("gemini-3.1-flash-lite", LlmModels.getDefaultModelForProvider(ProviderType.GEMINI).id)
    }

    @Test
    fun `existing Gemini selection remains resolvable`() {
        val model = LlmModels.getModelById("gemini-2.5-flash")
        assertNotNull(model)
        assertEquals(ProviderType.GEMINI, model?.provider)
    }

    @Test
    fun `every provider default belongs to its selectable catalog`() {
        for (provider in ProviderType.entries) {
            assertTrue(LlmModels.getModelsForProvider(provider).contains(LlmModels.getDefaultModelForProvider(provider)))
        }
    }
}
