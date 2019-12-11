package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module

@Module
internal abstract class OrderFulfillmentModule {
    @Binds
    abstract fun provideOrderFulfillmentPresenter(presenter: OrderFulfillmentPresenter):
            OrderFulfillmentContract.Presenter
}
