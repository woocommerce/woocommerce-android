package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsInterval
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AnalyticsStatsCardIdTest {
    @Test
    fun `given valid orders id, when parsed, then query fields are returned`() {
        val parsed = AnalyticsStatsCardId.parse(VALID_ID)

        assertThat(parsed).isEqualTo(
            AnalyticsStatsCardId(
                after = "2026-05-01",
                before = "2026-05-07",
                interval = AnalyticsInterval.DAY,
            )
        )
    }

    @Test
    fun `given id with currency segment, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day:currency:none"
            )
        ).isNull()
    }

    @Test
    fun `given legacy revenue id, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
    }

    @Test
    fun `given malformed section counts, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse("analytics_orders:after:2026-05-01:before:2026-05-07")
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse("$VALID_ID:extra:value")
        ).isNull()
    }

    @Test
    fun `given malformed labels, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_stats:after:2026-05-01:before:2026-05-07:interval:day"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:from:2026-05-01:before:2026-05-07:interval:day"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-05-01:until:2026-05-07:interval:day"
            )
        ).isNull()
    }

    @Test
    fun `given bad date formats, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-13-01:before:2026-05-07:interval:day"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-05-01T00:00:00:before:2026-05-07:interval:day"
            )
        ).isNull()
    }

    @Test
    fun `given invalid interval, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-05-01:before:2026-05-07:interval:quarter"
            )
        ).isNull()
    }

    @Test
    fun `given after date is after before date, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-05-07:before:2026-05-01:interval:day"
            )
        ).isNull()
    }

    @Test
    fun `given id exceeds length cap, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse("$VALID_ID:${"x".repeat(LONG_SUFFIX_LENGTH)}")
        ).isNull()
    }

    @Test
    fun `given analytics stats query, when converted to synthetic id, then it round trips`() {
        val query = AnalyticsStatsCardId(
            after = "2026-05-01",
            before = "2026-05-07",
            interval = AnalyticsInterval.MONTH,
        )

        assertThat(query.toSyntheticId())
            .isEqualTo("analytics_orders:after:2026-05-01:before:2026-05-07:interval:month")
        assertThat(AnalyticsStatsCardId.parse(query.toSyntheticId())).isEqualTo(query)
    }

    private companion object {
        private const val VALID_ID = "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day"
        private const val LONG_SUFFIX_LENGTH = 160
    }
}
