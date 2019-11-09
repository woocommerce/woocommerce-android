package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.ProductDetailFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductListFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ProductDetailFragmentModule::class,
    ProductListFragmentModule::class
])
object ProductsModule {
    @Module
    abstract class ProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductListModule::class])
        abstract fun productListFragment(): ProductListFragment
    }

    @Module
    abstract class ProductDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDetailModule::class])
        abstract fun productDetailFragment(): ProductDetailFragment
    }
}
