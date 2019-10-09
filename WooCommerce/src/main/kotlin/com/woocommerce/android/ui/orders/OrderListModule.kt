package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.list.OrderListFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderListModule {
    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
