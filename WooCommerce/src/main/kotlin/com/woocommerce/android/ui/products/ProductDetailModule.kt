package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ProductDetailModule {
    @FragmentScope
    @Binds
    abstract fun provideProductDetailPresenter(presenter: ProductDetailPresenter): ProductDetailContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun productDetailfragment(): ProductDetailFragment
}
