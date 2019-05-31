package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module

@Module
internal abstract class ProductDetailModule {
    @Binds
    abstract fun provideProductDetailPresenter(presenter: ProductDetailPresenter): ProductDetailContract.Presenter

    @Binds
    abstract fun provideUiMessageResolver(uiMessageResolver: ProductDetailUIMessageResolver): UIMessageResolver
}
