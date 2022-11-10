package com.woocommerce.android.e2e.screens

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.moremenu.MoreMenuScreen
import com.woocommerce.android.e2e.screens.mystore.MyStoreScreen
import com.woocommerce.android.e2e.screens.orders.OrderListScreen
import com.woocommerce.android.e2e.screens.products.ProductListScreen

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
