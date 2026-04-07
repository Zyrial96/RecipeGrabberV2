package com.recipegrabber.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {
    // Provider werden via Constructor Injection bereitgestellt
    // Keine expliziten Binds notwendig da @Inject Konstruktoren verwendet werden
}