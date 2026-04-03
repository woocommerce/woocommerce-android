package com.woocommerce.android.di

import com.automattic.eventhorizon.EventHorizon
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.EventHorizonAnalyticsEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EventHorizonModule {
    @Provides
    @Singleton
    fun provideEventHorizon(): EventHorizon {
        return EventHorizon { event ->
            AnalyticsTracker.track(
                EventHorizonAnalyticsEvent(event),
                event.analyticsProperties
            )
        }
    }
}
