package com.woocommerce.android.ui.products

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class ProductDetailModule {
    @ActivityScope
    @Binds
    abstract fun provideProductDetailPresenter(presenter: ProductDetailPresenter): ProductDetailContract.Presenter
}
