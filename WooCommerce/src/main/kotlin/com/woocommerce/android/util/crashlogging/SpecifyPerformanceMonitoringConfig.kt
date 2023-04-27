package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.BuildConfigWrapper
import javax.inject.Inject

class SpecifyPerformanceMonitoringConfig @Inject constructor(
    private val buildConfig: BuildConfigWrapper,
    private val analyticsWrapper: AnalyticsTrackerWrapper
) {

    private companion object {
        const val RELEASE_PERFORMANCE_MONITORING_SAMPLE_RATE = 0.1
        const val DEBUG_PERFORMANCE_MONITORING_SAMPLE_RATE = 1.0
    }

    operator fun invoke(): PerformanceMonitoringConfig {
        val sampleRate = if (buildConfig.debug) {
            DEBUG_PERFORMANCE_MONITORING_SAMPLE_RATE
        } else {
            RELEASE_PERFORMANCE_MONITORING_SAMPLE_RATE
        }
        val userEnabled = analyticsWrapper.sendUsageStats

        return if (!userEnabled) {
            PerformanceMonitoringConfig.Disabled
        } else {
            PerformanceMonitoringConfig.Enabled(sampleRate)
        }
    }
}
