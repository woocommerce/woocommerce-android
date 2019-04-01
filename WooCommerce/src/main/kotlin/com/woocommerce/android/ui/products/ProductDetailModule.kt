package com.woocommerce.android.ui.products

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module

@Module
internal abstract class ProductDetailModule {
    @ActivityScope
    @Binds
    abstract fun provideProductDetailPresenter(presenter: ProductDetailPresenter): ProductDetailContract.Presenter

    @ActivityScope
    @Binds
    abstract fun provideUiMessageResolver(uiMessageResolver: ProductDetailUIMessageResolver): UIMessageResolver
}
