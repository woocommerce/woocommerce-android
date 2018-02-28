package com.woocommerce.android.ui.orderlist

import com.woocommerce.android.di.ActivityScoped
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderListModule {
    @ActivityScoped
    @Binds
    abstract fun provideOrderListPresenter(orderListPresenter: OrderListPresenter): OrderListContract.Presenter

    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
