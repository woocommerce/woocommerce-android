package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module

@Module
internal abstract class OrderDetailModule {
    @Binds
    abstract fun provideOrderDetailPresenter(orderDetailPresenter: OrderDetailPresenter): OrderDetailContract.Presenter
}
