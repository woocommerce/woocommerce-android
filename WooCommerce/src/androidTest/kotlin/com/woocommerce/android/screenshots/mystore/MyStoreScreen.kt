package com.woocommerce.android.screenshots.mystore

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class MyStoreScreen : Screen(MY_STORE) {
    companion object {
        const val MY_STORE = R.id.my_store_refresh_layout
    }

    val stats = StatsComponent()
}
