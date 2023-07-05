package com.woocommerce.android.analytics

import com.woocommerce.shared.library.AnalyticsBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class AnalyticsModule {
    @Binds
    abstract fun provideSomeDependency(tracksAnalyticsBridge: TracksAnalyticsBridge): AnalyticsBridge
}
