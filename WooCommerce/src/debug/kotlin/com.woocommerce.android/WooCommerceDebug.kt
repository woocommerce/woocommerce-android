package com.woocommerce.android

import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponentDebug

open class WooCommerceDebug : WooCommerce() {
    override val component: AppComponent by lazy {
        DaggerAppComponentDebug.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
    }
}
