package com.recipegrabber.di

import com.recipegrabber.domain.llm.GeminiProvider
import com.recipegrabber.domain.llm.LlmProvider
import com.recipegrabber.domain.llm.OpenAiProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindOpenAiProvider(impl: OpenAiProvider): LlmProvider

    @Binds
    @Singleton
    abstract fun bindGeminiProvider(impl: GeminiProvider): LlmProvider
}
