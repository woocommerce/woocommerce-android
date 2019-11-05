package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class MockedProductDetailFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [MockedProductDetailModule::class])
    abstract fun productDetailfragment(): ProductDetailFragment
}
