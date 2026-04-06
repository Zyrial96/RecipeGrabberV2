package com.recipegrabber.domain.llm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmProviderFactory @Inject constructor(
    private val openAiProvider: OpenAiProvider,
    private val geminiProvider: GeminiProvider
) {
    fun create(type: ProviderType): LlmProvider {
        return when (type) {
            ProviderType.OPENAI -> openAiProvider
            ProviderType.GEMINI -> geminiProvider
        }
    }
}
