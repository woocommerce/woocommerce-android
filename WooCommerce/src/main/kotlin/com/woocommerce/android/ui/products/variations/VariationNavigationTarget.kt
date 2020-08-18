package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

/**
 * [VariationNavigationTarget] is a utility sealed class that identifies the different types of navigation that can
 * take place from the product details view while providing a common interface for managing them as a single type:
 *
 * Mostly used by [VariationDetailFragment] for handling navigation in the Products detail/product sub detail screen
 */
sealed class VariationNavigationTarget : Event() {
    data class ViewInventory(val inventoryData: InventoryData) : VariationNavigationTarget()
    data class ViewPricing(val pricingData: PricingData) : VariationNavigationTarget()
    data class ViewShipping(val remoteId: Long) : VariationNavigationTarget()
    data class ViewDescriptionEditor(val description: String, val title: String) : VariationNavigationTarget()
    data class ViewMenuOrder(val menuOrder: Int) : VariationNavigationTarget()
    data class ViewBottomSheet(val remoteId: Long) : VariationNavigationTarget()
    data class ShowImage(val image: Product.Image) : VariationNavigationTarget()
}
