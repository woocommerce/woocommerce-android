package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.extensions.semverCompareTo
import javax.inject.Inject

class GetShippingLabelSupport @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository
) {
    suspend operator fun invoke(): ShippingLabelSupport {
        orderDetailRepository.getWooShippingPluginInfo()
            .takeIf {
                val pluginVersion = it.version ?: "0.0.0"
                it.isOperational &&
                    pluginVersion.semverCompareTo(SUPPORTED_WC_SHIPPING_VERSION) >= 0
            }?.let {
                return ShippingLabelSupport.WC_SHIPPING_SUPPORTED
            }

        orderDetailRepository.getWooServicesPluginInfo()
            .takeIf {
                val pluginVersion = it.version ?: "0.0.0"
                it.isOperational && pluginVersion.semverCompareTo(SUPPORTED_WCS_VERSION) >= 0
            }?.let { return ShippingLabelSupport.WCS_SUPPORTED }

        return ShippingLabelSupport.NOT_SUPPORTED
    }

    companion object {
        // The required version to support shipping label creation
        const val SUPPORTED_WCS_VERSION = "1.25.11"
        const val SUPPORTED_WC_SHIPPING_VERSION = "1.0.6"
    }
}
