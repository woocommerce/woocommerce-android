package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WCS_COUNTRY
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WCS_CURRENCY
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ShippingLabelOnboardingRepositoryTest : BaseUnitTest() {
    private companion object {
        const val SITE_ID = 1
        val ELIGIBLE_ORDER_FOR_WCS_LABELS =
            Order.getEmptyOrder(Date(), Date()).copy(
                id = 123L,
                currency = SUPPORTED_WCS_CURRENCY,
                isCashPayment = false,
                items = OrderTestUtils.generateTestOrderItems(productId = 15),
            )
        val ORDER_PAYED_IN_EUROS = ELIGIBLE_ORDER_FOR_WCS_LABELS.copy(currency = "EUR")
    }

    private val orderDetailRepository: OrderDetailRepository = mock {
        on { hasVirtualProductsOnly(any()) } doReturn false
    }
    private val getShippingLabelSupport: GetShippingLabelSupport = mock {
        on { invoke() } doReturn ShippingLabelSupport.NOT_SUPPORTED
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()

    private val sut = ShippingLabelOnboardingRepository(
        orderDetailRepository,
        getShippingLabelSupport,
        appPrefsWrapper,
        selectedSite
    )

    @Before
    fun setup() {
        whenever(selectedSite.getSelectedSiteId()).thenReturn(SITE_ID)
    }

    @Test
    fun `Given WC shipping not ready, when order is eligible for shipping label, then show shipping banner is true`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertTrue(sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS))
    }

    @Test
    fun `Given WC shipping is active, when order is eligible for shipping label, then show shipping banner is false`() = testBlocking {
        givenShippingPluginSupport(ShippingLabelSupport.WCS_SUPPORTED)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse(sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS))
    }

    @Test
    fun `Given WC shipping not ready, when site is not in the US, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode("ES")

        assertFalse(sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS))
    }

    @Test
    fun `Given WC shipping not ready, when order is not in USD, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse(sut.shouldShowWcShippingBanner(ORDER_PAYED_IN_EUROS))
    }

    @Test
    fun `Given WC shipping not ready, when order has only virtual products, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)
        givenOrderHasVirtualProductsOnly()

        assertFalse(sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS))
    }

    @Test
    fun `Given WC shipping not ready, when install WCS banner is dismissed, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)
        givenWcShippingBannerIsDismissed(dismissed = true)

        assertFalse(sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS))
    }

    private suspend fun givenShippingPluginSupport(support: ShippingLabelSupport) {
        whenever(getShippingLabelSupport()).thenReturn(support)
    }

    private fun givenStoreCountryCode(countryCode: String) {
        whenever(orderDetailRepository.getStoreCountryCode())
            .thenReturn(countryCode)
    }

    private suspend fun givenOrderHasVirtualProductsOnly() {
        whenever(orderDetailRepository.hasVirtualProductsOnly(any())).thenReturn(true)
    }

    private fun givenWcShippingBannerIsDismissed(dismissed: Boolean) {
        whenever(appPrefsWrapper.getWcShippingBannerDismissed(SITE_ID))
            .thenReturn(dismissed)
    }
}
