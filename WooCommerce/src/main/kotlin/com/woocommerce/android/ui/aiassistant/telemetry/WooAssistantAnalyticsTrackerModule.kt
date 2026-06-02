package com.woocommerce.android.ui.aiassistant.telemetry

import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WooAssistantAnalyticsTrackerModule {
    @Binds
    @Singleton
    internal abstract fun bindAssistantTelemetryTracker(
        impl: WooAssistantAnalyticsTracker,
    ): AssistantTelemetryTracker
}
