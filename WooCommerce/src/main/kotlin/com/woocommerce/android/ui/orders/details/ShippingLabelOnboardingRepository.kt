package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.WooPlugin
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
        const val SUPPORTED_WCS_CURRENCY = "USD"
        const val SUPPORTED_WCS_COUNTRY = "US"
    }

    val isShippingPluginReady: Boolean by lazy { isShippingLabelSupported() }

    fun shouldShowWcShippingBanner(order: Order, eligibleForIpp: Boolean): Boolean =
        !isShippingPluginReady &&
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
    private fun isShippingLabelSupported(): Boolean {
        orderDetailRepository.getWooServicesPluginInfo()
            .takeIf {
                val pluginVersion = it.version ?: "0.0.0"
                it.isPluginReady() && pluginVersion.semverCompareTo(SUPPORTED_WCS_VERSION) >= 0
            }?.let { return true }

        orderDetailRepository.getWooShippingPluginInfo()
            .takeIf { it.isPluginReady() && FeatureFlag.NEW_SHIPPING_SUPPORT.isEnabled() }
            ?.let { return true }

        return false
    }

    private fun WooPlugin.isPluginReady() = isInstalled && isActive
}
