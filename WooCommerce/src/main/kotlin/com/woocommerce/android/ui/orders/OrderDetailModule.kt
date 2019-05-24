package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderDetailModule {
    @Binds
    abstract fun provideOrderDetailPresenter(orderDetailPresenter: OrderDetailPresenter): OrderDetailContract.Presenter

    @ContributesAndroidInjector
    abstract fun orderDetailfragment(): OrderDetailFragment
}
