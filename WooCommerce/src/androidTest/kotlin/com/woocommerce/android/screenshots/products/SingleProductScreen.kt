package com.woocommerce.android.screenshots.products

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.ProductData
import com.woocommerce.android.screenshots.util.Screen

class SingleProductScreen : Screen {
    companion object {
        const val PRODUCT_DETAIL_CONTAINER = R.id.productDetail_root
    }

    constructor() : super(PRODUCT_DETAIL_CONTAINER)

    fun goBackToProductsScreen(): ProductListScreen {
        pressBack()
        return ProductListScreen()
    }

    fun assertProductDetails(productData: ProductData): SingleProductScreen {
        // check the product details page against the mock data
        return SingleProductScreen()
    }
}
