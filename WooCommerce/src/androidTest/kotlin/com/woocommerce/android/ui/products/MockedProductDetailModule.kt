package com.woocommerce.android.ui.products

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MockedProductDetailModule {
    @ContributesAndroidInjector
    abstract fun productDetailfragment(): ProductDetailFragment
}
