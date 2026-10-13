package com.recipegrabber.domain.llm

data class LlmModel(
    val id: String,
    val name: String,
    val description: String,
    val provider: ProviderType,
    val isVisionCapable: Boolean = false,
    val contextWindow: Int = 4096
)

object LlmModels {
    // OpenAI Models
    val GPT_4O = LlmModel(
        id = "gpt-4o",
        name = "GPT-4o",
        description = "Bestes Allround-Modell mit Vision",
        provider = ProviderType.OPENAI,
        isVisionCapable = true,
        contextWindow = 128000
    )
    
    val GPT_4O_MINI = LlmModel(
        id = "gpt-4o-mini",
        name = "GPT-4o Mini",
        description = "Schnell und günstig",
        provider = ProviderType.OPENAI,
        isVisionCapable = true,
        contextWindow = 128000
    )
    
    // Google Models
    val GEMINI_38_FLASH = LlmModel(
        id = "gemini-3.8-flash",
        name = "Gemini 3.8 Flash",
        description = "Text- und Videoanalyse",
        provider = ProviderType.GEMINI,
        isVisionCapable = true,
        contextWindow = 1048576
    )
    
    val GEMINI_31_FLASH_LITE = LlmModel(
        id = "gemini-3.1-flash-lite",
        name = "Gemini 3.1 Flash-Lite",
        description = "Leichtgewichtige Text- und Videoanalyse",
        provider = ProviderType.GEMINI,
        isVisionCapable = true,
        contextWindow = 1048576
    )

    val GEMINI_25_FLASH = LlmModel(
        id = "gemini-2.5-flash",
        name = "Gemini 2.5 Flash",
        description = "Fuer bestehende Projekte mit Modellzugriff",
        provider = ProviderType.GEMINI,
        isVisionCapable = true,
        contextWindow = 1000000
    )
    
    // Claude Models
    val CLAUDE_35_SONNET = LlmModel(
        id = "claude-3-5-sonnet-20241022",
        name = "Claude 3.5 Sonnet",
        description = "Analytik-Master",
        provider = ProviderType.CLAUDE,
        isVisionCapable = true,
        contextWindow = 200000
    )
    
    val CLAUDE_3_HAIKU = LlmModel(
        id = "claude-3-haiku-20240307",
        name = "Claude 3 Haiku",
        description = "Effizienz",
        provider = ProviderType.CLAUDE,
        isVisionCapable = true,
        contextWindow = 200000
    )
    
    // Kimi Models
    val KIMI_K2_5 = LlmModel(
        id = "moonshot-v1-128k",
        name = "Moonshot V1 128K",
        description = "Spezialist für lange Kontexte",
        provider = ProviderType.KIMI,
        isVisionCapable = false,
        contextWindow = 128000
    )
    
    val ALL_MODELS = listOf(
        GPT_4O, GPT_4O_MINI,
        GEMINI_38_FLASH, GEMINI_31_FLASH_LITE, GEMINI_25_FLASH,
        CLAUDE_35_SONNET, CLAUDE_3_HAIKU,
        KIMI_K2_5
    )
    
    fun getModelsForProvider(provider: ProviderType): List<LlmModel> {
        return ALL_MODELS.filter { it.provider == provider }
    }
    
    fun getModelById(id: String): LlmModel? {
        return ALL_MODELS.find { it.id == id }
    }
    
    fun getDefaultModelForProvider(provider: ProviderType): LlmModel {
        return when (provider) {
            ProviderType.OPENAI -> GPT_4O
            ProviderType.GEMINI -> GEMINI_31_FLASH_LITE
            ProviderType.CLAUDE -> CLAUDE_35_SONNET
            ProviderType.KIMI -> KIMI_K2_5
        }
    }
}
