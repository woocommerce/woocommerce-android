package com.woocommerce.android.screenshots

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.moremenu.MoreMenuScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.util.Screen

class TabNavComponent : Screen(MY_STORE_BUTTON) {
    companion object {
        const val MY_STORE_BUTTON = R.id.dashboard
        const val ORDERS_BUTTON = R.id.orders
        const val PRODUCTS_BUTTON = R.id.products
        const val MORE_MENU_BUTTON = R.id.moreMenu
    }

    fun gotoMyStoreScreen(): MyStoreScreen {
        clickOn(MY_STORE_BUTTON)
        return MyStoreScreen()
    }

    fun gotoOrdersScreen(): OrderListScreen {
        clickOn(ORDERS_BUTTON)
        return OrderListScreen()
    }

    fun gotoProductsScreen(): ProductListScreen {
        clickOn(PRODUCTS_BUTTON)
        return ProductListScreen()
    }

    fun gotoMoreMenuScreen(): MoreMenuScreen {
        clickOn(MORE_MENU_BUTTON)
        return MoreMenuScreen()
    }
}
