package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class ShippingLabelOnboardingRepository @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val appSharedPrefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
) {
    companion object {
        const val SUPPORTED_WC_SHIPPING_VERSION = "1.0.6"
        const val SUPPORTED_WC_SHIPPING_CURRENCY = "USD"
        const val SUPPORTED_WC_SHIPPING_COUNTRY = "US"
    }

    val shippingPluginSupport: ShippingLabelSupport by lazy { getShippingLabelSupport() }

    fun shouldShowWcShippingBanner(order: Order, eligibleForIpp: Boolean): Boolean =
        !shippingPluginSupport.isSupported() &&
            orderDetailRepository.getStoreCountryCode() == SUPPORTED_WC_SHIPPING_COUNTRY &&
            order.currency == SUPPORTED_WC_SHIPPING_CURRENCY &&
            !order.isCashPayment &&
            !eligibleForIpp &&
            !hasVirtualProductsOnly(order) &&
            !appSharedPrefs.getWcShippingBannerDismissed(selectedSite.getSelectedSiteId())

    fun markWcShippingBannerAsDismissed() {
        appSharedPrefs.setWcShippingBannerDismissed(dismissed = true, selectedSite.getSelectedSiteId())
    }

    private fun hasVirtualProductsOnly(order: Order): Boolean {
        return if (order.items.isNotEmpty()) {
            val remoteProductIds = order.getProductIds()
            orderDetailRepository.hasVirtualProductsOnly(remoteProductIds)
        } else {
            false
        }
    }

    private fun getShippingLabelSupport(): ShippingLabelSupport {
        orderDetailRepository.getWooShippingPluginInfo()
            .takeIf {
                val pluginVersion = it.version ?: "0.0.0"
                it.isOperational &&
                    pluginVersion.semverCompareTo(SUPPORTED_WC_SHIPPING_VERSION) >= 0
            }?.let {
                return ShippingLabelSupport.WC_SHIPPING_SUPPORTED
            }

        return ShippingLabelSupport.NOT_SUPPORTED
    }

    enum class ShippingLabelSupport {
        NOT_SUPPORTED,
        WC_SHIPPING_SUPPORTED;

        fun isSupported() = this == WC_SHIPPING_SUPPORTED
        fun isWooShippingSupported() = this == WC_SHIPPING_SUPPORTED
    }
}
