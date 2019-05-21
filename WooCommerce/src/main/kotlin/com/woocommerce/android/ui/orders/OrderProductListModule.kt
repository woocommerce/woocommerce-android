package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderProductListModule {
    @Binds
    abstract fun provideOrderProductListPresenter(
        orderProductListPresenter: OrderProductListPresenter
    ): OrderProductListContract.Presenter

    @ContributesAndroidInjector
    abstract fun orderProductListFragment(): OrderProductListFragment
}
