package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.ProductHelper
import com.woocommerce.android.util.CurrencyFormatter
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Singleton

@Module
class ProductHelperModule {
    @Singleton
    @Provides
    fun provideProductHelper(
        productStore: WCProductStore,
        selectedSite: SelectedSite,
        dispatcher: Dispatcher
    ): ProductHelper = ProductHelper(productStore, selectedSite, dispatcher)
}
