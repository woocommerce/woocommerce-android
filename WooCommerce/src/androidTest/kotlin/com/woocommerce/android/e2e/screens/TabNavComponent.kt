package com.woocommerce.android.e2e.screens

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.moremenu.MoreMenuScreen
import com.woocommerce.android.e2e.screens.mystore.DashboardScreen
import com.woocommerce.android.e2e.screens.orders.OrderListScreen
import com.woocommerce.android.e2e.screens.products.ProductListScreen

class TabNavComponent : Screen(R.id.dashboard) {
    fun gotoMyStoreScreen(): DashboardScreen {
        clickOn(R.id.dashboard)
        return DashboardScreen()
    }

    fun gotoOrdersScreen(): OrderListScreen {
        clickOn(R.id.orders)
        return OrderListScreen()
    }

    fun gotoProductsScreen(): ProductListScreen {
        clickOn(R.id.products)
        return ProductListScreen()
    }

    fun gotoMoreMenuScreen(): MoreMenuScreen {
        clickOn(R.id.moreMenu)
        return MoreMenuScreen()
    }
}
