package com.woocommerce.android.ui.orders.list

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderListModule {
    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
