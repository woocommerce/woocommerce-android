package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.config.RemoteConfigRepository
import javax.inject.Inject

class SpecifyPerformanceMonitoringConfig @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsWrapper: AnalyticsTrackerWrapper
) {
    operator fun invoke(): PerformanceMonitoringConfig {
        val sampleRate = remoteConfigRepository.getPerformanceMonitoringSampleRate()
        val userEnabled = analyticsWrapper.sendUsageStats

        return when {
            !userEnabled || sampleRate <= 0.0 || sampleRate > 1.0 -> PerformanceMonitoringConfig.Disabled
            else -> PerformanceMonitoringConfig.Enabled(sampleRate)
        }
    }
}
