package com.woocommerce.android.aiassistant.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AiAssistantModule {
    @Provides
    @Singleton
    @AiAssistantJson
    fun provideAiAssistantJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
}
