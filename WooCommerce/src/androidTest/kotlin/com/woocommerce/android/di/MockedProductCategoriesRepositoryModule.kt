package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.MockedProductCategoriesRepository
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCProductStore

@Module
object MockedProductCategoriesRepositoryModule {
    @JvmStatic
    @Provides
    fun provideProductCategoriesRepository(
        dispatcher: Dispatcher,
        productStore: WCProductStore,
        selectedSite: SelectedSite
    ): MockedProductCategoriesRepository {
        return MockedProductCategoriesRepository(dispatcher, productStore, selectedSite)
    }
}
