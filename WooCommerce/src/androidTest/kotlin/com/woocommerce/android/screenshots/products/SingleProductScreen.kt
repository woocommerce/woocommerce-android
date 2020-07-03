package com.woocommerce.android.screenshots.products

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SingleProductScreen : Screen(PRODUCT_DETAIL_CONTAINER) {
    companion object {
        const val PRODUCT_DETAIL_CONTAINER = R.id.productDetail_root
    }

    fun goBackToProductsScreen(): ProductListScreen {
        actionOps.pressBack()
        return ProductListScreen()
    }

    fun goBackToProductList(): ProductListScreen {
        actionOps.pressBack()
        return ProductListScreen()
    }
}
