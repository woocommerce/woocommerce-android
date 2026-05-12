package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.runtime.AgenticLoopAssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.telemetry.SystemClock
import com.woocommerce.android.aiassistant.telemetry.WallSystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AssistantRuntimeModule {
    @Binds
    @Singleton
    internal abstract fun bindAssistantRuntime(runtime: AgenticLoopAssistantRuntime): AssistantRuntime

    @Binds
    @Singleton
    internal abstract fun bindSystemClock(clock: WallSystemClock): SystemClock
}
