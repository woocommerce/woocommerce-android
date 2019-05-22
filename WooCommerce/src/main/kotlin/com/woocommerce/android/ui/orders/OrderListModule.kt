package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderListModule {
    @Binds
    abstract fun provideOrderListPresenter(orderListPresenter: OrderListPresenter): OrderListContract.Presenter

    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
