package com.woocommerce.android.di

import com.woocommerce.android.WooCommerce

object Injector {
    @JvmStatic fun get(): AppComponent = WooCommerce.instance.component
}
