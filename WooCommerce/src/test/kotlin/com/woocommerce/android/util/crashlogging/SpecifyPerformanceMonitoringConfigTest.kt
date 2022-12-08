package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.config.RemoteConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SpecifyPerformanceMonitoringConfigTest {
    private val remoteConfig: RemoteConfigRepository = mock {
        on { getPerformanceMonitoringSampleRate() } doReturn 0.5
    }
    private val analytics: AnalyticsTrackerWrapper = mock {
        on { sendUsageStats } doReturn true
    }

    private val sut = SpecifyPerformanceMonitoringConfig(remoteConfig, analytics)

    @Test
    fun `if user disabled tracking, disable performance monitoring`() {
        whenever(analytics.sendUsageStats).thenReturn(false)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `if monitoring sample rate is zero, disable performance monitoring`() {
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).doReturn(0.0)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `if monitoring sample rate is above 1, disable performance monitoring`() {
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).doReturn(1.2)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `if monitoring sample rate is below 0, disable performance monitoring`() {
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).doReturn(-123.0)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Disabled)
    }

    @Test
    fun `if monitoring sample rate is between 0 and 1 and user enabled tracking, enable performance monitoring`() {
        whenever(analytics.sendUsageStats).thenReturn(true)
        whenever(remoteConfig.getPerformanceMonitoringSampleRate()).doReturn(0.7)

        val result = sut.invoke()

        assertThat(result).isEqualTo(PerformanceMonitoringConfig.Enabled(0.7))
    }
}
