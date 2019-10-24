package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ProductDetailFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [ProductDetailModule::class])
    abstract fun productDetailFragment(): ProductDetailFragment
}
