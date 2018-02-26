package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderListModule {
    @FragmentScope
    @Binds
    abstract fun provideOrderListPresenter(orderListPresenter: OrderListPresenter): OrderListContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
