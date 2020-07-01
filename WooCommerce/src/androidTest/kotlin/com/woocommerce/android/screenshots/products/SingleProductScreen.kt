package com.woocommerce.android.screenshots.products

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SingleProductScreen : Screen {
    companion object {
        const val PRODUCT_DETAIL_CONTAINER = R.id.productDetail_root
        const val INVENTORY_DETAIL = R.id.textPropertyName
        const val MANAGE_STOCK_SWITCH = R.id.manageStock_switch
        const val QUANTITY_FIELD = R.id.edit_text
        const val DONE_AND_UPDATE_BTN = R.id.menu_done
        const val UPDATE_BTN = R.id.productDetail_root
        const val STOCK_STATUS_SPINNER = R.id.spinner_edit_text
        const val STOCK_STATUS_ITEM = R.id.productDetail_root
    }

    constructor() : super(PRODUCT_DETAIL_CONTAINER)

    fun goBackToProductsScreen(): ProductListScreen {
        pressBack()
        return ProductListScreen()
    }

    private fun changeStockStatus(randomInteger: Int, recyclerViewID: Int) {
        clickOn(STOCK_STATUS_SPINNER)
        selectItemAtIndexInRecyclerView1(randomInteger, recyclerViewID)
        clickOn(STOCK_STATUS_ITEM)
    }

    fun goBackToProductList(): ProductListScreen {
        pressBack()
        return ProductListScreen()
    }
}
