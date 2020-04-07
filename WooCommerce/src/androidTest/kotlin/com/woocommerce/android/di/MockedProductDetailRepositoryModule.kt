package com.woocommerce.android.di

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.MockedProductDetailRepository
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCTaxStore

@Module
object MockedProductDetailRepositoryModule {
    private var productModel: WCProductModel? = null

    fun setProduct(productModel: WCProductModel) {
        this.productModel = productModel
    }

    @JvmStatic
    @Provides
    fun provideProductDetailRepository(
        dispatcher: Dispatcher,
        productStore: WCProductStore,
        selectedSite: SelectedSite,
        taxStore: WCTaxStore
    ): MockedProductDetailRepository {
        val repository = MockedProductDetailRepository(dispatcher, productStore, selectedSite, taxStore)
        repository.product = productModel?.toAppModel()
        return repository
    }
}
