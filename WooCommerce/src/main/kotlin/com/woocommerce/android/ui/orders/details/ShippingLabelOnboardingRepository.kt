package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class ShippingLabelOnboardingRepository @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val appSharedPrefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
) {
    companion object {
        // The required version to support shipping label creation
        const val SUPPORTED_WCS_VERSION = "1.25.11"
        const val SUPPORTED_WC_SHIPPING_VERSION = "1.0.6"
        const val SUPPORTED_WCS_CURRENCY = "USD"
        const val SUPPORTED_WCS_COUNTRY = "US"
    }

    val shippingPluginSupport: ShippingLabelSupport by lazy { getShippingLabelSupport() }

    fun shouldShowWcShippingBanner(order: Order, eligibleForIpp: Boolean): Boolean =
        !shippingPluginSupport.isSupported() &&
            orderDetailRepository.getStoreCountryCode() == SUPPORTED_WCS_COUNTRY &&
            order.currency == SUPPORTED_WCS_CURRENCY &&
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

    @Suppress("ReturnCount")
    private fun getShippingLabelSupport(): ShippingLabelSupport {
        orderDetailRepository.getWooServicesPluginInfo()
            .takeIf {
                val pluginVersion = it.version ?: "0.0.0"
                it.isOperational && pluginVersion.semverCompareTo(SUPPORTED_WCS_VERSION) >= 0
            }?.let { return ShippingLabelSupport.WCS_SUPPORTED }

        orderDetailRepository.getWooShippingPluginInfo()
            .takeIf {
                val pluginVersion = it.version ?: "0.0.0"
                it.isOperational &&
                    pluginVersion.semverCompareTo(SUPPORTED_WC_SHIPPING_VERSION) >= 0
            }?.let {
                return if (FeatureFlag.REVAMP_WOO_SHIPPING.isEnabled()) {
                    ShippingLabelSupport.WC_SHIPPING_SUPPORTED
                } else {
                    ShippingLabelSupport.WCS_SUPPORTED
                }
            }

        return ShippingLabelSupport.NOT_SUPPORTED
    }

    enum class ShippingLabelSupport {
        NOT_SUPPORTED,
        WC_SHIPPING_SUPPORTED,
        WCS_SUPPORTED;

        fun isSupported() = this == WCS_SUPPORTED || this == WC_SHIPPING_SUPPORTED
        fun isWooShippingSupported() = this == WC_SHIPPING_SUPPORTED
    }
}
