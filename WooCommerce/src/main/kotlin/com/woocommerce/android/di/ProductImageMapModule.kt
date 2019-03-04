package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ProductImageMap
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Singleton

@Module
class ProductImageMapModule {
    @Provides
    @Singleton
    fun provideSelectedSite(selectedSite: SelectedSite, productStore: WCProductStore) =
            ProductImageMap(selectedSite, productStore)
}
