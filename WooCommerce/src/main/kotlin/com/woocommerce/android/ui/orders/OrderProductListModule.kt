package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module

@Module
internal abstract class OrderProductListModule {
    @Binds
    abstract fun provideOrderProductListPresenter(
        orderProductListPresenter: OrderProductListPresenter
    ): OrderProductListContract.Presenter
}
