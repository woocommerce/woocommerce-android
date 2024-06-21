package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.SubscriptionDetails
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel.ShippingData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

/**
 * [VariationNavigationTarget] is a utility sealed class that identifies the different types of navigation that can
 * take place from the product details view while providing a common interface for managing them as a single type:
 *
 * Mostly used by [VariationDetailFragment] for handling navigation in the Products detail/product sub detail screen
 */
sealed class VariationNavigationTarget : Event() {
    data class ViewInventory(val inventoryData: InventoryData, val sku: String) : VariationNavigationTarget()
    data class ViewPricing(val pricingData: PricingData) : VariationNavigationTarget()
    data class ViewShipping(val shippingData: ShippingData) : VariationNavigationTarget()
    data class ViewDescriptionEditor(val description: String, val title: String) : VariationNavigationTarget()
    data class ViewMediaUploadErrors(val remoteId: Long) : VariationNavigationTarget()

    data class ViewProductSubscriptionExpiration(
        val subscription: SubscriptionDetails
    ) : VariationNavigationTarget()

    data class ViewVariationSubscriptionTrial(
        val subscription: SubscriptionDetails
    ) : VariationNavigationTarget()

    data class ViewProductQuantityRules(
        val quantityRules: QuantityRules,
        val exitAnalyticsEvent: AnalyticsEvent
    ) : VariationNavigationTarget()
    data class ViewAttributes(
        val remoteProductId: Long,
        val remoteVariationId: Long
    ) : VariationNavigationTarget()

    data class ViewImageGallery(
        val remoteId: Long,
        val images: List<Image>,
        val showChooser: Boolean = false,
        val selectedImage: Image? = null
    ) : VariationNavigationTarget()
}
