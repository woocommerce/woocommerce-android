package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderFulfillmentModule {
    @Binds
    abstract fun provideOrderFulfillmentPresenter(presenter: OrderFulfillmentPresenter):
            OrderFulfillmentContract.Presenter

    @ContributesAndroidInjector
    abstract fun orderFulfillmentFragment(): OrderFulfillmentFragment
}
