package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WooAssistantCardActionNavigatorTest {
    @Test
    fun `given open order action, when mapped, then order details direction is returned`() {
        val direction = requireNotNull(
            AssistantCardAction.OpenOrder(remoteOrderId = 123L).toNavDirections()
        )

        assertThat(direction.actionId).isEqualTo(R.id.action_global_orderDetailFragment)
        assertThat(direction).isEqualTo(
            NavGraphMainDirections.actionGlobalOrderDetailFragment(
                orderId = 123L,
                ignoreTwoPaneLayoutLogic = true,
            )
        )
    }

    @Test
    fun `given open product action, when mapped, then product details direction is returned`() {
        val direction = requireNotNull(
            AssistantCardAction.OpenProduct(remoteProductId = 456L).toNavDirections()
        )

        assertThat(direction.actionId).isEqualTo(R.id.action_global_productDetailFragment)
        assertThat(direction).isEqualTo(
            NavGraphMainDirections.actionGlobalProductDetailFragment(
                mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId = 456L),
            )
        )
    }

    @Test
    fun `given open analytics action, when mapped, then analytics direction is returned`() {
        val direction = requireNotNull(
            AssistantCardAction.OpenAnalytics(after = "2026-05-01", before = "2026-05-07")
                .toNavDirections(locale = Locale.US)
        )

        assertThat(direction.actionId).isEqualTo(R.id.action_global_analytics)
        assertThat(direction.rangeSelection()).isNotNull()
    }

    @Test
    fun `given analytics date range, when mapped, then hub range preserves requested dates`() {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01:00"))
        try {
            val range = requireNotNull(
                analyticsDatesToStatsTimeRangeSelection(
                    after = "2026-05-01",
                    before = "2026-05-06",
                    locale = Locale.US,
                )
            )
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+01:00"), Locale.US)

            calendar.time = range.currentRange.start
            assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2026)
            assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.MAY)
            assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)

            calendar.time = range.currentRange.end
            assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2026)
            assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.MAY)
            assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(6)
            assertThat(range.currentRangeDescription).contains("May 1")
            assertThat(range.currentRangeDescription).contains("6, 2026")
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun `given invalid analytics date, when range is mapped, then null is returned`() {
        val range = analyticsDatesToStatsTimeRangeSelection(
            after = "not-a-date",
            before = "2026-05-01",
            locale = Locale.US,
        )

        assertThat(range).isNull()
    }

    private fun androidx.navigation.NavDirections.rangeSelection(): StatsTimeRangeSelection {
        val field = javaClass.getDeclaredField("rangeSelection")
        field.isAccessible = true
        return field.get(this) as StatsTimeRangeSelection
    }
}
