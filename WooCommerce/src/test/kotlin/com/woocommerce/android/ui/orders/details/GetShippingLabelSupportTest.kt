package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.ui.orders.details.GetShippingLabelSupport.Companion.SUPPORTED_WCS_VERSION
import com.woocommerce.android.ui.orders.details.GetShippingLabelSupport.Companion.SUPPORTED_WC_SHIPPING_VERSION
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetShippingLabelSupportTest {
    private val orderDetailRepository: OrderDetailRepository = mock {
        on { getWooShippingPluginInfo() } doReturn NOT_INSTALLED_PLUGIN
        on { getWooServicesPluginInfo() } doReturn NOT_INSTALLED_PLUGIN
    }
    private val getShippingLabelSupport = GetShippingLabelSupport(orderDetailRepository)

    @Test
    fun `given legacy shipping is active at minimum version, when support is checked, then legacy is supported`() {
        givenWooServicesPlugin(version = SUPPORTED_WCS_VERSION)

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.WCS_SUPPORTED)
    }

    @Test
    fun `given Woo Shipping is active at minimum version, when support is checked, then Woo Shipping is supported`() {
        givenWooShippingPlugin(version = SUPPORTED_WC_SHIPPING_VERSION)

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.WC_SHIPPING_SUPPORTED)
    }

    @Test
    fun `given both plugins are supported, when support is checked, then Woo Shipping takes precedence`() {
        givenWooShippingPlugin(version = SUPPORTED_WC_SHIPPING_VERSION)
        givenWooServicesPlugin(version = SUPPORTED_WCS_VERSION)

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.WC_SHIPPING_SUPPORTED)
    }

    @Test
    fun `given both plugins are installed but inactive, when support is checked, then neither is supported`() {
        givenWooShippingPlugin(active = false, version = SUPPORTED_WC_SHIPPING_VERSION)
        givenWooServicesPlugin(active = false, version = SUPPORTED_WCS_VERSION)

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.NOT_SUPPORTED)
    }

    @Test
    fun `given Woo Shipping is below minimum version, when legacy is supported, then fall back to legacy`() {
        givenWooShippingPlugin(version = "1.0.5")
        givenWooServicesPlugin(version = SUPPORTED_WCS_VERSION)

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.WCS_SUPPORTED)
    }

    @Test
    fun `given legacy shipping is below minimum version, when support is checked, then it is not supported`() {
        givenWooServicesPlugin(version = "1.25.10")

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.NOT_SUPPORTED)
    }

    @Test
    fun `given active plugins have null versions, when support is checked, then neither is supported`() {
        givenWooShippingPlugin(version = null)
        givenWooServicesPlugin(version = null)

        val result = getShippingLabelSupport()

        assertThat(result).isEqualTo(ShippingLabelSupport.NOT_SUPPORTED)
    }

    private fun givenWooShippingPlugin(
        installed: Boolean = true,
        active: Boolean = true,
        version: String?
    ) {
        whenever(orderDetailRepository.getWooShippingPluginInfo())
            .thenReturn(WooPlugin(installed, active, version))
    }

    private fun givenWooServicesPlugin(
        installed: Boolean = true,
        active: Boolean = true,
        version: String?
    ) {
        whenever(orderDetailRepository.getWooServicesPluginInfo())
            .thenReturn(WooPlugin(installed, active, version))
    }

    private companion object {
        val NOT_INSTALLED_PLUGIN = WooPlugin(isInstalled = false, isActive = false, version = null)
    }
}
