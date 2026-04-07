package com.recipegrabber.domain.llm

import com.recipegrabber.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmProviderFactory @Inject constructor(
    private val openAiProvider: OpenAiProvider,
    private val geminiProvider: GeminiProvider,
    private val claudeProvider: ClaudeProvider,
    private val kimiProvider: KimiProvider,
    private val preferencesRepository: PreferencesRepository
) {
    fun create(type: ProviderType): LlmProvider {
        return when (type) {
            ProviderType.OPENAI -> openAiProvider
            ProviderType.GEMINI -> geminiProvider
            ProviderType.CLAUDE -> claudeProvider
            ProviderType.KIMI -> kimiProvider
        }
    }

    suspend fun createFromPreferences(): LlmProvider {
        val type = preferencesRepository.llmProviderType.first()
        return create(type)
    }
}