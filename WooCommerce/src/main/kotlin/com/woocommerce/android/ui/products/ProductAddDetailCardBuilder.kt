package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider

class ProductAddDetailCardBuilder(
    viewModel: ProductDetailViewModel,
    resources: ResourceProvider,
    currencyFormatter: CurrencyFormatter,
    parameters: SiteParameters
) : ProductDetailCardBuilder(viewModel, resources, currencyFormatter, parameters) {
    override fun getProductTitlePlaceholder(): Int =
        R.string.product_add_card_placeholder_title

    override fun getProductInventory(product: Product): ProductProperty? {
        if (product.sku.isEmpty()) return null
        return super.getProductInventory(product)
    }
}
