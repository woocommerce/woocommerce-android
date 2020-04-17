package com.woocommerce.android.ui.products

import android.view.View
import com.woocommerce.android.model.Product
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import java.lang.ref.WeakReference

/**
 * [ProductNavigationTarget] is a utility sealed class that identifies the different types of navigation that can
 * take place from the product details view while providing a common interface for managing them as a single type:
 *
 * Mostly used by [ProductDetailFragment] for handling navigation in the Products detail/product sub detail screen
 */
sealed class ProductNavigationTarget : Event() {
    data class ShareProduct(val url: String, val title: String) : ProductNavigationTarget()
    data class ViewProductVariations(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductInventory(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductPricing(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductShipping(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductDescriptionEditor(val description: String, val title: String) : ProductNavigationTarget()
    data class ViewProductPurchaseNoteEditor(val purchaseNote: String, val title: String) : ProductNavigationTarget()
    data class ViewProductShortDescriptionEditor(val shortDescription: String, val title: String) :
            ProductNavigationTarget()
    data class ViewProductImages(
        val product: Product,
        val imageModel: Product.Image? = null,
        val clickedImage: WeakReference<View>
    ) : ProductNavigationTarget()
    data class ViewProductImageChooser(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductSettings(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductStatus(val status: ProductStatus?) : ProductNavigationTarget()
    data class ViewProductVisibility(val visibility: ProductVisibility?, val isFeatured: Boolean) :
            ProductNavigationTarget()
    data class ViewProductSlug(val slug: String) : ProductNavigationTarget()
    object ExitProduct : ProductNavigationTarget()
}
