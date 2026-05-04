package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.config.WooCommerceAssistantSystemPromptProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AssistantConfigModule {
    @Binds
    @Singleton
    internal abstract fun bindAssistantSystemPromptProvider(
        provider: WooCommerceAssistantSystemPromptProvider,
    ): AssistantSystemPromptProvider
}
