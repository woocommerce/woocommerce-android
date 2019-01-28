package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Singleton

@Module
class CurrencyModule {
    @Singleton
    @Provides
    fun provideCurrencyFormatter(
        wooCommerceStore: WooCommerceStore,
        selectedSite: SelectedSite
    ): CurrencyFormatter = CurrencyFormatter(wooCommerceStore, selectedSite)
}
