package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.OrdersModule.OrderListFragmentModule
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.orders.list.OrderListModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    OrderListFragmentModule::class
])
object OrdersModule {
    @Module
    abstract class OrderListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [OrderListModule::class])
        abstract fun orderListFragment(): OrderListFragment
    }
}
