package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.runtime.AgenticLoopAssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AssistantRuntimeModule {
    @Binds
    @Singleton
    internal abstract fun bindAssistantRuntime(runtime: AgenticLoopAssistantRuntime): AssistantRuntime

    companion object {
        @Provides
        @Singleton
        internal fun provideAssistantTelemetryTimeSource(): TimeSource = TimeSource.Monotonic
    }
}
