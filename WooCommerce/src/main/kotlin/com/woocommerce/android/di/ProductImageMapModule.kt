package com.woocommerce.android.di

import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Singleton

@Module
class ProductImageMapModule {
    @Provides
    @Singleton
    fun provideProductImageMap(
        selectedSite: SelectedSite,
        productStore: WCProductStore,
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
        coroutineDispatchers: CoroutineDispatchers,
    ) = ProductImageMap(selectedSite, productStore, appCoroutineScope, coroutineDispatchers)
}
