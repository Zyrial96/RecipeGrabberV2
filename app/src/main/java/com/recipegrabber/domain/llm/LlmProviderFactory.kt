package com.recipegrabber.domain.llm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmProviderFactory @Inject constructor(
    private val openAiProvider: OpenAiProvider,
    private val geminiProvider: GeminiProvider,
    private val claudeProvider: ClaudeProvider,
    private val kimiProvider: KimiProvider
) {
    fun create(type: ProviderType): LlmProvider {
        return when (type) {
            ProviderType.OPENAI -> openAiProvider
            ProviderType.GEMINI -> geminiProvider
            ProviderType.CLAUDE -> claudeProvider
            ProviderType.KIMI -> kimiProvider
        }
    }
}