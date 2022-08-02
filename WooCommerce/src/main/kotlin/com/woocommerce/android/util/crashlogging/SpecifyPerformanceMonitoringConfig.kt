package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.config.RemoteConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class SpecifyPerformanceMonitoringConfig @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsWrapper: AnalyticsTrackerWrapper
) {
    operator fun invoke(): PerformanceMonitoringConfig {
        return runBlocking {
            val sampleRate = remoteConfigRepository.observePerformanceMonitoringSampleRate().first()
            val userEnabled = analyticsWrapper.sendUsageStats

            when {
                !userEnabled || sampleRate <= 0.0 || sampleRate > 1.0 -> PerformanceMonitoringConfig.Disabled
                else -> PerformanceMonitoringConfig.Enabled(sampleRate)
            }
        }
    }
}
