package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.list.OrderListContractNew
import com.woocommerce.android.ui.orders.list.OrderListFragmentNew
import com.woocommerce.android.ui.orders.list.OrderListPresenterNew
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderListModule {
    @Binds
    abstract fun provideOrderListPresenter(orderListPresenter: OrderListPresenterNew): OrderListContractNew.Presenter

    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragmentNew
}
