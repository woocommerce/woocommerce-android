package com.woocommerce.android

import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponentTest

open class WooCommerceTest : WooCommerce() {
    override val component: AppComponent by lazy {
        DaggerAppComponentTest.builder()
                .application(this)
                .build()
    }
}
