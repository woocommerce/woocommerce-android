package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCProductStore

@Module
object MockedProductTagsModule {
    @JvmStatic
    @Provides
    fun provideProductTagsRepository(
        dispatcher: Dispatcher,
        productStore: WCProductStore,
        selectedSite: SelectedSite
    ): MockedProductTagsRepository {
        return MockedProductTagsRepository(dispatcher, productStore, selectedSite)
    }
}
