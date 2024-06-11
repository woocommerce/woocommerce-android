package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WCS_COUNTRY
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository.Companion.SUPPORTED_WCS_CURRENCY
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        const val DEFAULT_SUPPORTED_WCS_VERSION = "1.25.11"
        val ELIGIBLE_ORDER_FOR_WCS_LABELS =
            Order.getEmptyOrder(Date(), Date()).copy(
                id = 123L,
                currency = SUPPORTED_WCS_CURRENCY,
                isCashPayment = false,
                items = OrderTestUtils.generateTestOrderItems(productId = 15),
            )
        val ORDER_PAYED_IN_EUROS = ELIGIBLE_ORDER_FOR_WCS_LABELS.copy(currency = "EUR")
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
    fun `Given WC shipping not ready, when order is eligible for shipping label, then show shipping banner is true`() {
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertTrue {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping is active, when order is eligible for shipping label, then show shipping banner is false`() {
        givenWCLegacyShippingPlugin(installed = true, active = true)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when site is not in the US, then show shipping banner is false`() {
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenStoreCountryCode("ES")

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order is not in USD, then show shipping banner is false`() {
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ORDER_PAYED_IN_EUROS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order has only virtual products, then show shipping banner is false`() {
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)
        givenOrderHasVirtualProductsOnly()

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC shipping not ready, when order is eligible for SL and IPP, then show shipping banner is false`() {
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = true)
        }
    }

    @Test
    fun `Given WC shipping not ready, when install WCS banner is dismissed, then show shipping banner is false`() {
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenStoreCountryCode(SUPPORTED_WCS_COUNTRY)
        givenWcShippingBannerIsDismissed(dismissed = true)

        assertFalse {
            sut.shouldShowWcShippingBanner(ELIGIBLE_ORDER_FOR_WCS_LABELS, eligibleForIpp = false)
        }
    }

    @Test
    fun `Given WC legacy shipping is ready, then isShippingPluginReady is true`() {
        // Given
        givenWCLegacyShippingPlugin(installed = true, active = true)
        givenWCShippingPlugin(installed = false, active = false)

        // When
        val isShippingPluginReady = sut.isShippingPluginReady

        // Then
        assertThat(isShippingPluginReady).isTrue
    }

    @Test
    fun `Given WC legacy shipping is not ready and Shipping plugin is, then isShippingPluginReady is true`() {
        // Given
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenWCShippingPlugin(installed = true, active = true)

        // When
        val isShippingPluginReady = sut.isShippingPluginReady

        // Then
        assertThat(isShippingPluginReady).isTrue
    }

    @Test
    fun `Given both WC legacy and shipping plugins are not ready, then isShippingPluginReady is false`() {
        // Given
        givenWCLegacyShippingPlugin(installed = false, active = false)
        givenWCShippingPlugin(installed = false, active = false)

        // When
        val isShippingPluginReady = sut.isShippingPluginReady

        // Then
        assertThat(isShippingPluginReady).isFalse
    }

    private fun givenWCLegacyShippingPlugin(
        installed: Boolean,
        active: Boolean,
        version: String = DEFAULT_SUPPORTED_WCS_VERSION
    ) {
        whenever(orderDetailRepository.getWooServicesPluginInfo())
            .thenReturn(WooPlugin(installed, active, version))
    }

    private fun givenWCShippingPlugin(
        installed: Boolean,
        active: Boolean,
        version: String = DEFAULT_SUPPORTED_WCS_VERSION
    ) {
        whenever(orderDetailRepository.getWooShippingPluginInfo())
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

    private fun givenWcShippingBannerIsDismissed(dismissed: Boolean) {
        whenever(appPrefsWrapper.getWcShippingBannerDismissed(SITE_ID))
            .thenReturn(dismissed)
    }
}
