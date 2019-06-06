package com.woocommerce.android.di

import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Singleton

@Module
class ProductImageMapModule {
    @Provides
    @Singleton
    fun provideProductImageMap(selectedSite: SelectedSite, productStore: WCProductStore) =
            ProductImageMap(selectedSite, productStore)
}
