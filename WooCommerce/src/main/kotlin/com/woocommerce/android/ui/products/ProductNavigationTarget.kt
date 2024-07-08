package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.model.Component
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.model.SubscriptionDetails
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.grouped.GroupedProductListType
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorFlow
import com.woocommerce.android.ui.products.selector.ProductSourceForTracking
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.products.settings.ProductVisibility
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

/**
 * [ProductNavigationTarget] is a utility sealed class that identifies the different types of navigation that can
 * take place from the product details view while providing a common interface for managing them as a single type:
 *
 * Mostly used by [ProductDetailFragment] for handling navigation in the Products detail/product sub detail screen
 */
sealed class ProductNavigationTarget : Event() {
    data class ShareProduct(val url: String, val title: String) : ProductNavigationTarget()
    data class ShareProductWithMessage(
        val title: String,
        val subject: String
    ) : ProductNavigationTarget()

    data class ShareProductWithAI(
        val permalink: String,
        val title: String,
        val description: String? = null
    ) : ProductNavigationTarget()

    data class ViewProductVariations(
        val remoteId: Long,
        val productSelectorFlow: ProductSelectorFlow = ProductSelectorFlow.Undefined,
    ) : ProductNavigationTarget()

    data class ViewProductInventory(
        val inventoryData: InventoryData,
        val sku: String,
        val productType: ProductType
    ) : ProductNavigationTarget()

    object ViewProductAttributes : ProductNavigationTarget()
    data class ViewProductPricing(val pricingData: PricingData) : ProductNavigationTarget()
    data class ViewProductShipping(val shippingData: ShippingData) : ProductNavigationTarget()
    data class ViewProductExternalLink(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductDescriptionEditor(val description: String, val title: String, val productTitle: String) :
        ProductNavigationTarget()

    data class ViewProductPurchaseNoteEditor(
        val purchaseNote: String,
        val title: String,
        val caption: String
    ) : ProductNavigationTarget()

    data class ViewProductShortDescriptionEditor(val shortDescription: String, val title: String) :
        ProductNavigationTarget()

    data class ViewProductImageGallery(
        val remoteId: Long,
        val images: List<Image>,
        val showChooser: Boolean = false,
        val selectedImage: Image? = null
    ) : ProductNavigationTarget()

    data class ViewMediaUploadErrors(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductSettings(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductStatus(val status: ProductStatus?) : ProductNavigationTarget()
    data class ViewProductCatalogVisibility(val catalogVisibility: ProductCatalogVisibility?, val isFeatured: Boolean) :
        ProductNavigationTarget()

    data class ViewProductVisibility(
        val isApplicationPasswordsLogin: Boolean,
        val visibility: ProductVisibility?,
        val password: String?
    ) : ProductNavigationTarget()

    data class ViewProductSlug(val slug: String) : ProductNavigationTarget()
    data class ViewProductMenuOrder(val menuOrder: Int) : ProductNavigationTarget()
    object ExitProduct : ProductNavigationTarget()
    data class ViewProductCategories(val remoteId: Long) : ProductNavigationTarget()
    object AddProductCategory : ProductNavigationTarget()
    data class EditCategory(val category: ProductCategory) : ProductNavigationTarget()
    data class ViewProductTags(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductDetailBottomSheet(val productType: ProductType) : ProductNavigationTarget()
    data class ViewProductTypes(
        val isAddProduct: Boolean,
        val currentProductType: String,
        val isCurrentProductVirtual: Boolean
    ) : ProductNavigationTarget()

    data class ViewProductReviews(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductAdd(val source: AddProductSource) : ProductNavigationTarget()
    data class ViewGroupedProducts(val remoteId: Long, val groupedProductIds: List<Long>) : ProductNavigationTarget()
    data class ViewLinkedProducts(val remoteId: Long) : ProductNavigationTarget()
    data class ViewProductSelectionList(
        val remoteId: Long,
        val groupedProductType: GroupedProductListType,
        val excludedProductIds: List<Long>
    ) : ProductNavigationTarget()

    object ViewProductDownloads : ProductNavigationTarget()
    object ViewProductDownloadsSettings : ProductNavigationTarget()
    data class ViewProductDownloadDetails(
        val isEditing: Boolean,
        val file: ProductFile
    ) : ProductNavigationTarget()

    object ViewProductAddonsDetails : ProductNavigationTarget()
    object AddProductDownloadableFile : ProductNavigationTarget()
    data class AddProductAttribute(
        val isVariationCreation: Boolean = false
    ) : ProductNavigationTarget()

    data class AddProductAttributeTerms(
        val attributeId: Long,
        val attributeName: String,
        val isNewAttribute: Boolean,
        val isVariationCreation: Boolean
    ) : ProductNavigationTarget()

    data class RenameProductAttribute(
        val attributeName: String
    ) : ProductNavigationTarget()

    data class NavigateToVariationSelector(
        val productId: Long,
        val selectedVariationIds: Set<Long>,
        val productSelectorFlow: ProductSelectorFlow = ProductSelectorFlow.Undefined,
        val productSourceForTracking: ProductSourceForTracking,
        val selectionMode: ProductSelectorViewModel.SelectionMode,
        val screenMode: VariationSelectorViewModel.ScreenMode,
    ) : ProductNavigationTarget()

    data class NavigateToProductConfiguration(val productId: Long) : ProductNavigationTarget()
    data class EditProductConfiguration(
        val itemId: Long,
        val productId: Long,
        val configuration: ProductConfiguration
    ) : ProductNavigationTarget()

    data class NavigateToProductFilter(
        val stockStatus: String?,
        val productType: String?,
        val productStatus: String?,
        val productCategory: String?,
        val productCategoryName: String?,
        val restrictions: List<ProductRestriction>
    ) : ProductNavigationTarget()

    data class ViewProductSubscriptionExpiration(
        val subscription: SubscriptionDetails
    ) : ProductNavigationTarget()

    data class ViewProductSubscriptionFreeTrial(
        val subscription: SubscriptionDetails
    ) : ProductNavigationTarget()

    data class ViewProductQuantityRules(
        val quantityRules: QuantityRules,
        val exitAnalyticsEvent: AnalyticsEvent
    ) : ProductNavigationTarget()

    data class ViewBundleProducts(val productId: Long) : ProductNavigationTarget()

    data class ViewProductComponents(val components: List<Component>) : ProductNavigationTarget()

    data class ViewFirstProductCelebration(
        val permalink: String,
        val productName: String
    ) : ProductNavigationTarget()
}
