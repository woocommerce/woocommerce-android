package com.woocommerce.android.ui.products

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ProductDetailModule {
    @Binds
    abstract fun provideProductDetailPresenter(presenter: ProductDetailPresenter): ProductDetailContract.Presenter

    @ContributesAndroidInjector
    abstract fun productDetailfragment(): ProductDetailFragment
}
