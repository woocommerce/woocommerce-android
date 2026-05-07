package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.moremenu.customer.GetCustomerWithStats
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WooAssistantCardActionNavigatorTest {
    private val getCustomerWithStats: GetCustomerWithStats = mock()
    private val navigator = WooAssistantCardActionNavigator(getCustomerWithStats)

    @Test
    fun `given open order action, when mapped, then order details direction is returned`() = runTest {
        val direction = requireNotNull(
            navigator.directionFor(AssistantCardAction.OpenOrder(remoteOrderId = 123L))
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
    fun `given open product action, when mapped, then product details direction is returned`() = runTest {
        val direction = requireNotNull(
            navigator.directionFor(AssistantCardAction.OpenProduct(remoteProductId = 456L))
        )

        assertThat(direction.actionId).isEqualTo(R.id.action_global_productDetailFragment)
        assertThat(direction).isEqualTo(
            NavGraphMainDirections.actionGlobalProductDetailFragment(
                mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId = 456L),
            )
        )
    }

    @Test
    fun `given open analytics action, when mapped, then analytics direction is returned`() = runTest {
        val direction = requireNotNull(
            navigator.directionFor(
                AssistantCardAction.OpenAnalytics(after = "2026-05-01", before = "2026-05-07"),
                locale = Locale.US,
            )
        )

        assertThat(direction.actionId).isEqualTo(R.id.action_global_analytics)
        assertThat(direction.rangeSelection()).isNotNull()
    }

    @Test
    fun `given open customer action, when customer resolves, then customer details direction is returned`() = runTest {
        val customerWithAnalytics = customerWithAnalytics(remoteCustomerId = 789L)
        whenever(getCustomerWithStats.invoke(789L, null)).thenReturn(Result.success(customerWithAnalytics))

        val direction = navigator.directionFor(AssistantCardAction.OpenCustomer(remoteCustomerId = 789L))

        val expected = NavGraphMainDirections.actionGlobalCustomerDetailsFragment(customerWithAnalytics)
        assertThat(direction?.actionId).isEqualTo(R.id.action_global_customerDetailsFragment)
        assertThat(direction).isEqualTo(expected)
        verify(getCustomerWithStats).invoke(789L, null)
    }

    @Test
    fun `given open customer action, when customer does not resolve, then null direction is returned`() = runTest {
        whenever(getCustomerWithStats.invoke(789L, null)).thenReturn(Result.failure(IllegalStateException("missing")))

        val direction = navigator.directionFor(AssistantCardAction.OpenCustomer(remoteCustomerId = 789L))

        assertThat(direction).isNull()
        verify(getCustomerWithStats).invoke(789L, null)
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

    private fun customerWithAnalytics(remoteCustomerId: Long) = CustomerWithAnalytics(
        remoteCustomerId = remoteCustomerId,
        analyticsCustomerId = null,
        firstName = "Ada",
        lastName = "Lovelace",
        username = "ada",
        email = "ada@example.com",
        phone = "",
        lastActive = null,
        ordersCount = null,
        totalSpend = null,
        averageOrderValue = null,
        registeredDate = "2026-05-01",
        billingAddress = Address.EMPTY,
        shippingAddress = Address.EMPTY,
    )
}
