package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class ShippingLabelOnboardingRepository @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val getShippingLabelSupport: GetShippingLabelSupport,
    private val appSharedPrefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
) {
    suspend fun shouldShowWcShippingBanner(order: Order): Boolean =
        !getShippingLabelSupport().isSupported() &&
            orderDetailRepository.getStoreCountryCode() == SUPPORTED_WCS_COUNTRY &&
            order.currency == SUPPORTED_WCS_CURRENCY &&
            !order.isCashPayment &&
            !hasVirtualProductsOnly(order) &&
            !appSharedPrefs.getWcShippingBannerDismissed(selectedSite.getSelectedSiteId())

    fun markWcShippingBannerAsDismissed() {
        appSharedPrefs.setWcShippingBannerDismissed(dismissed = true, selectedSite.getSelectedSiteId())
    }

    private suspend fun hasVirtualProductsOnly(order: Order): Boolean {
        return if (order.items.isNotEmpty()) {
            val remoteProductIds = order.getProductIds()
            orderDetailRepository.hasVirtualProductsOnly(remoteProductIds)
        } else {
            false
        }
    }

    companion object {
        const val SUPPORTED_WCS_CURRENCY = "USD"
        const val SUPPORTED_WCS_COUNTRY = "US"
    }
}
