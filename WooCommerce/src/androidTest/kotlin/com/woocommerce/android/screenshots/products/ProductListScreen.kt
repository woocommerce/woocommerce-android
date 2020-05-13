package com.woocommerce.android.screenshots.products

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.util.Screen

class ProductListScreen : Screen {
    companion object {
        const val LIST_VIEW = R.id.productsRecycler
        const val LIST_ITEM = R.id.linearLayout
    }

    val tabBar = TabNavComponent()

    constructor(): super(LIST_VIEW)

    fun selectProduct(index: Int): SingleProductScreen {
        selectItemAtIndexInRecyclerView(index, LIST_VIEW, LIST_ITEM)
        return SingleProductScreen()
    }
}
