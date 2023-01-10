package com.woocommerce.android.ui.analytics

import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.sync.UpdateAnalyticsHubStats
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

internal class UpdateAnalyticsHubStatsTest {
    private lateinit var analyticsRepository: AnalyticsRepository

    private lateinit var sut: UpdateAnalyticsHubStats

    @Before
    fun setUp() {
        analyticsRepository = mock()
        sut = UpdateAnalyticsHubStats(
            analyticsRepository = analyticsRepository
        )
    }

    @Test
    fun `initial test`() {
        assertThat(sut).isNotNull
    }
}
