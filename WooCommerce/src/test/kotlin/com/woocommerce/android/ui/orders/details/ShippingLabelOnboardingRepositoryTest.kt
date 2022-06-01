package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WCS_COUNTRY
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WCS_CURRENCY
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShippingLabelOnboardingRepositoryTest : BaseUnitTest() {
    private companion object {
        const val DEFAULT_SUPPORTED_WCS_VERSION = "1.25.11"
        val ELIGIBLE_ORDER_FOR_WCS_LABELS =
            Order.EMPTY.copy(
                id = 123L,
                currency = SUPPORTED_WCS_CURRENCY,
                isCashPayment = false,
                items = OrderTestUtils.generateTestOrderItems(productId = 15),
            )
        val ORDER_PAYED_IN_EUROS = ELIGIBLE_ORDER_FOR_WCS_LABELS.copy(currency = "EUR")
        val CASH_PAYMENT_ORDER = ELIGIBLE_ORDER_FOR_WCS_LABELS.copy(isCashPayment = true)
    }

    private val orderDetailRepository: OrderDetailRepository = mock()

    private val sut = ShippingLabelOnboardingRepository(orderDetailRepository)

    @Test
    fun `Given WC shipping not ready, when order is eligible for shipping label, then show shipping banner is true`() {
        givenWcShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertTrue {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping is active, when order is eligible for shipping label, then show shipping banner is false`() {
        givenWcShippingPlugin(installed = true, active = true)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when site is not in the US, then show shipping banner is false`() {
        givenWcShippingPlugin(installed = false, active = false)
        givenStoreCountryCode("ES")

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order is not in USD, then show shipping banner is false`() {
        givenWcShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ORDER_PAYED_IN_EUROS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order has only virtual products, then show shipping banner is false`() {
        givenWcShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)
        givenOrderHasVirtualProductsOnly()

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order is eligible for SL and IPP, then show shipping banner is false`() {
        givenWcShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = true)
        }
    }

    private fun givenWcShippingPlugin(
        installed: Boolean,
        active: Boolean,
        version: String = DEFAULT_SUPPORTED_WCS_VERSION
    ) {
        whenever(orderDetailRepository.getWooServicesPluginInfo())
            .thenReturn(WooPlugin(installed, active, version))
    }

    private fun givenStoreCountryCode(countryCode: String) {
        whenever(orderDetailRepository.getStoreCountryCode())
            .thenReturn(countryCode)
    }

    private fun givenOrderHasVirtualProductsOnly() {
        whenever(orderDetailRepository.hasVirtualProductsOnly(any()))
            .thenReturn(true)
    }
}
