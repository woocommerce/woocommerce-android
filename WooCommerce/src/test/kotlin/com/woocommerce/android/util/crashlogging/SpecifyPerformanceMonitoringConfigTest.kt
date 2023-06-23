package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.BuildConfigWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SpecifyPerformanceMonitoringConfigTest {
    private val buildConfig: BuildConfigWrapper = mock()
    private val analytics: AnalyticsTrackerWrapper = mock()

    private val sut = SpecifyPerformanceMonitoringConfig(buildConfig, analytics)

    @Test
    fun `given the user disabled tracking, disable performance monitoring`() {
        whenever(analytics.sendUsageStats).thenReturn(false)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `given the debug app, disable performance monitoring`() {
        whenever(buildConfig.debug).doReturn(true)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `given the app is not debug and user did not disabled tracking, enable performance monitoring`() {
        whenever(analytics.sendUsageStats).thenReturn(true)
        whenever(buildConfig.debug).doReturn(false)

        val result = sut.invoke()

        assertThat(result).isEqualTo(
            PerformanceMonitoringConfig.Enabled(
                sampleRate = 0.02,
                profilesSampleRate = 0.01
            )
        )
    }
}
