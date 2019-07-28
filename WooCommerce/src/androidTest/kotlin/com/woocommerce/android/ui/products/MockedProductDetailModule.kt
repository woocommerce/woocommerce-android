package com.woocommerce.android.ui.products

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.model.WCProductModel

@Module
abstract class MockedProductDetailModule {
    @Module
    companion object {
        private var product: WCProductModel? = null

        fun setMockProduct(product: WCProductModel) {
            this.product = product
        }
    }

    @ContributesAndroidInjector
    abstract fun productDetailfragment(): ProductDetailFragment
}
