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
        const val PERFORMANCE_MONITORING_SAMPLE_RATE = 0.01
        const val PROFILING_SAMPLE_RATE = 0.01
    }

    operator fun invoke(): PerformanceMonitoringConfig {
        val userEnabled = analyticsWrapper.sendUsageStats

        return if (!userEnabled || buildConfig.debug) {
            PerformanceMonitoringConfig.Disabled
        } else {
            PerformanceMonitoringConfig.Enabled(
                sampleRate = PERFORMANCE_MONITORING_SAMPLE_RATE,
                profilesSampleRate = PROFILING_SAMPLE_RATE
            )
        }
    }
}
