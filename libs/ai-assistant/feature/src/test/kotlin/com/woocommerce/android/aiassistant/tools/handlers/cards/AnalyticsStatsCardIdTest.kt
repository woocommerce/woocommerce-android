package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsInterval
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AnalyticsStatsCardIdTest {
    @Test
    fun `given valid id with currency, when parsed, then query fields are returned`() {
        val parsed = AnalyticsStatsCardId.parse(VALID_ID)

        assertThat(parsed).isEqualTo(
            AnalyticsStatsCardId(
                kind = AnalyticsStatsKind.Revenue,
                after = "2026-05-01",
                before = "2026-05-07",
                interval = AnalyticsInterval.DAY,
                currency = "USD",
            )
        )
    }

    @Test
    fun `given valid id with no currency, when parsed, then currency is null`() {
        val parsed = AnalyticsStatsCardId.parse(
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:week:currency:none"
        )

        assertThat(parsed).isEqualTo(
            AnalyticsStatsCardId(
                kind = AnalyticsStatsKind.Revenue,
                after = "2026-05-01",
                before = "2026-05-07",
                interval = AnalyticsInterval.WEEK,
                currency = null,
            )
        )
    }

    @Test
    fun `given valid orders id without currency segment, when parsed, then query fields are returned`() {
        val parsed = AnalyticsStatsCardId.parse(
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day"
        )

        assertThat(parsed).isEqualTo(
            AnalyticsStatsCardId(
                kind = AnalyticsStatsKind.Orders,
                after = "2026-05-01",
                before = "2026-05-07",
                interval = AnalyticsInterval.DAY,
                currency = null,
            )
        )
    }

    @Test
    fun `given valid orders id with currency none, when parsed, then currency is null`() {
        val parsed = AnalyticsStatsCardId.parse(
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day:currency:none"
        )

        assertThat(parsed).isEqualTo(
            AnalyticsStatsCardId(
                kind = AnalyticsStatsKind.Orders,
                after = "2026-05-01",
                before = "2026-05-07",
                interval = AnalyticsInterval.DAY,
                currency = null,
            )
        )
    }

    @Test
    fun `given revenue id without currency segment, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day"
            )
        ).isNull()
    }

    @Test
    fun `given orders id with concrete currency, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
    }

    @Test
    fun `given malformed section counts, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse("analytics_revenue:after:2026-05-01:before:2026-05-07")
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse("$VALID_ID:extra:value")
        ).isNull()
    }

    @Test
    fun `given malformed labels, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_stats:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:from:2026-05-01:before:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:until:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
    }

    @Test
    fun `given bad date formats, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-13-01:before:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01T00:00:00:before:2026-05-07:interval:day:currency:USD"
            )
        ).isNull()
    }

    @Test
    fun `given invalid interval, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:quarter:currency:USD"
            )
        ).isNull()
    }

    @Test
    fun `given invalid currencies, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:usd"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:US"
            )
        ).isNull()
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:US1"
            )
        ).isNull()
    }

    @Test
    fun `given after date is after before date, when parsed, then id is rejected`() {
        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-07:before:2026-05-01:interval:day:currency:USD"
            )
        ).isNull()
    }

    @Test
    fun `given id exceeds length cap, when parsed, then id is rejected`() {
        val longCurrency = "U".repeat(LONG_CURRENCY_LENGTH)

        assertThat(
            AnalyticsStatsCardId.parse(
                "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:$longCurrency"
            )
        ).isNull()
    }

    @Test
    fun `given analytics stats query, when converted to synthetic id, then it round trips`() {
        val query = AnalyticsStatsCardId(
            kind = AnalyticsStatsKind.Revenue,
            after = "2026-05-01",
            before = "2026-05-07",
            interval = AnalyticsInterval.MONTH,
            currency = "USD",
        )

        assertThat(AnalyticsStatsCardId.parse(query.toSyntheticId())).isEqualTo(query)
    }

    @Test
    fun `given analytics stats query without currency, when converted to synthetic id, then it round trips`() {
        val query = AnalyticsStatsCardId(
            kind = AnalyticsStatsKind.Revenue,
            after = "2026-05-01",
            before = "2026-05-07",
            interval = AnalyticsInterval.YEAR,
            currency = null,
        )

        assertThat(query.toSyntheticId()).endsWith(":currency:none")
        assertThat(AnalyticsStatsCardId.parse(query.toSyntheticId())).isEqualTo(query)
    }

    @Test
    fun `given orders analytics stats query, when converted to synthetic id, then it omits currency and round trips`() {
        val query = AnalyticsStatsCardId(
            kind = AnalyticsStatsKind.Orders,
            after = "2026-05-01",
            before = "2026-05-07",
            interval = AnalyticsInterval.DAY,
            currency = null,
        )

        assertThat(query.toSyntheticId())
            .isEqualTo("analytics_orders:after:2026-05-01:before:2026-05-07:interval:day")
        assertThat(AnalyticsStatsCardId.parse(query.toSyntheticId())).isEqualTo(query)
    }

    private companion object {
        private const val VALID_ID =
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
        private const val LONG_CURRENCY_LENGTH = 120
    }
}
