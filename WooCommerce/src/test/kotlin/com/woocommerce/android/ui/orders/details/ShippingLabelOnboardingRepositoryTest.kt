package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WC_SHIPPING_COUNTRY
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WC_SHIPPING_CURRENCY
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.ShippingLabelSupport
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ShippingLabelOnboardingRepositoryTest : BaseUnitTest() {
    private companion object {
        const val SITE_ID = 1
        const val DEFAULT_SUPPORTED_WC_SHIPPING_VERSION = "1.0.6"
        val ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS =
            Order.getEmptyOrder(Date(), Date()).copy(
                id = 123L,
                currency = SUPPORTED_WC_SHIPPING_CURRENCY,
                isCashPayment = false,
                items = OrderTestUtils.generateTestOrderItems(productId = 15),
            )
        val ORDER_PAYED_IN_EUROS = ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS.copy(currency = "EUR")
    }

    private val orderDetailRepository: OrderDetailRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()

    private val sut = ShippingLabelOnboardingRepository(
        orderDetailRepository,
        appPrefsWrapper,
        selectedSite
    )

    @Before
    fun setup() {
        givenWCShippingPlugin(installed = false, active = false)
        whenever(selectedSite.getSelectedSiteId()).thenReturn(SITE_ID)
    }

    @Test
    fun `Given WC shipping not ready, when order is eligible for shipping label, then show shipping banner is true`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WC_SHIPPING_COUNTRY)

        assertTrue {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping is active, when order is eligible for shipping label, then show shipping banner is false`() = testBlocking {
        givenWCShippingPlugin(installed = true, active = true)
        givenStoreCountryCode(SUPPORTED_WC_SHIPPING_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when site is not in the US, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode("ES")

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order is not in USD, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WC_SHIPPING_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ORDER_PAYED_IN_EUROS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order has only virtual products, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WC_SHIPPING_COUNTRY)
        givenOrderHasVirtualProductsOnly()

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order is eligible for SL and IPP, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WC_SHIPPING_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS, eligibleForIpp = true)
        }
    }

    @Test
    fun `given WC shipping not ready, when install WC Shipping banner is dismissed, then show shipping banner is false`() = testBlocking {
        givenStoreCountryCode(SUPPORTED_WC_SHIPPING_COUNTRY)
        givenWcShippingBannerIsDismissed(dismissed = true)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WC_SHIPPING_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `given WC shipping is ready, when checking shipping plugin support, then isShippingPluginReady is true`() {
        // Given
        givenWCShippingPlugin(installed = true, active = true)

        // When
        val isShippingPluginReady = sut.shippingPluginSupport.isSupported()

        // Then
        assertThat(isShippingPluginReady).isTrue
    }

    @Test
    fun `given WC shipping is ready, when checking shipping plugin support, then shippingPluginSupport uses WC Shipping`() {
        // Given
        givenWCShippingPlugin(installed = true, active = true)

        // When
        val isShippingPluginReady = sut.shippingPluginSupport.isSupported()

        // Then
        assertThat(isShippingPluginReady).isTrue
        assertThat(sut.shippingPluginSupport).isEqualTo(ShippingLabelSupport.WC_SHIPPING_SUPPORTED)
    }

    @Test
    fun `given WC shipping is not ready, when checking shipping plugin support, then isShippingPluginReady is false`() {
        // Given
        givenWCShippingPlugin(installed = false, active = false)

        // When
        val isShippingPluginReady = sut.shippingPluginSupport.isSupported()

        // Then
        assertThat(isShippingPluginReady).isFalse
    }

    private fun givenWCShippingPlugin(
        installed: Boolean,
        active: Boolean,
        version: String = DEFAULT_SUPPORTED_WC_SHIPPING_VERSION
    ) {
        whenever(orderDetailRepository.getWooShippingPluginInfo())
            .thenReturn(WooPlugin(installed, active, version))
    }

    private fun givenStoreCountryCode(countryCode: String) {
        whenever(orderDetailRepository.getStoreCountryCode())
            .thenReturn(countryCode)
    }

    private fun givenOrderHasVirtualProductsOnly() {
        runBlocking {
            whenever(orderDetailRepository.hasVirtualProductsOnly(any())).thenReturn(true)
        }
    }

    private fun givenWcShippingBannerIsDismissed(dismissed: Boolean) {
        whenever(appPrefsWrapper.getWcShippingBannerDismissed(SITE_ID))
            .thenReturn(dismissed)
    }
}
