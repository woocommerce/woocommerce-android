package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ProductListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [ProductListModule::class])
    abstract fun productListFragment(): ProductListFragment
}
