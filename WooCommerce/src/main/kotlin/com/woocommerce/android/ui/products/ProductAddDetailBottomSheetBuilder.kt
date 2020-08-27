package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductInventory
import com.woocommerce.android.viewmodel.ResourceProvider

class ProductAddDetailBottomSheetBuilder
constructor(resources: ResourceProvider) : ProductDetailBottomSheetBuilder(resources) {
    override fun getSimpleProductList(product: Product): List<ProductDetailBottomSheetUiItem> {
        return listOfNotNull(
            product.getInventory(),
            product.getShipping(),
            product.getCategories(),
            product.getTags(),
            product.getShortDescription()
        )
    }

    private fun Product.getInventory(): ProductDetailBottomSheetUiItem? {
        if (hasInventory) return null
        return ProductDetailBottomSheetUiItem(
            ProductDetailBottomSheetType.PRODUCT_INVENTORY,
            ViewProductInventory(
                InventoryData(
                    sku = sku,
                    isStockManaged = isStockManaged,
                    isSoldIndividually = isSoldIndividually,
                    stockStatus = stockStatus,
                    stockQuantity = stockQuantity,
                    backorderStatus = backorderStatus
                )
                , sku
            )
            ,
            Stat.PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
        )
    }
}
