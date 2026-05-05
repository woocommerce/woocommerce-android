package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils.getNormalizedTimezone
import java.util.Calendar
import java.util.Locale

class WooAssistantCardActionNavigatorTest {
    @Test
    fun `given open order action, when mapped, then order details direction is returned`() {
        val direction = requireNotNull(
            AssistantCardAction.OpenOrder(remoteOrderId = 123L).toNavDirections(site = site())
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
            AssistantCardAction.OpenProduct(remoteProductId = 456L).toNavDirections(site = site())
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
                .toNavDirections(site = site(), locale = Locale.US)
        )

        assertThat(direction.actionId).isEqualTo(R.id.action_global_analytics)
        assertThat(direction.rangeSelection()).isNotNull()
    }

    @Test
    fun `given negative utc site timezone, when analytics range is mapped, then local dates do not shift backward`() {
        val site = SiteModel().apply { timezone = "-7" }
        val range = requireNotNull(
            analyticsDatesToStatsTimeRangeSelection(
                after = "2026-05-01",
                before = "2026-05-01",
                site = site,
                locale = Locale.US,
            )
        )
        val calendar = Calendar.getInstance(getNormalizedTimezone(site.timezone), Locale.US)

        calendar.time = range.currentRange.start
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.MAY)
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)

        calendar.time = range.currentRange.end
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.MAY)
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
    }

    @Test
    fun `given invalid analytics date, when range is mapped, then null is returned`() {
        val site = SiteModel().apply { timezone = "-7" }

        val range = analyticsDatesToStatsTimeRangeSelection(
            after = "not-a-date",
            before = "2026-05-01",
            site = site,
            locale = Locale.US,
        )

        assertThat(range).isNull()
    }

    private fun site() = SiteModel().apply { timezone = "-7" }

    private fun androidx.navigation.NavDirections.rangeSelection(): StatsTimeRangeSelection {
        val field = javaClass.getDeclaredField("rangeSelection")
        field.isAccessible = true
        return field.get(this) as StatsTimeRangeSelection
    }
}
