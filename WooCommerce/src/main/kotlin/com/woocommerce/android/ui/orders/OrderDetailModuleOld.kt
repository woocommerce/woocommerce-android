package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module

@Module
internal abstract class OrderDetailModuleOld {
    @Binds
    abstract fun provideOrderDetailPresenter(orderDetailPresenter: OrderDetailPresenter): OrderDetailContract.Presenter
}
