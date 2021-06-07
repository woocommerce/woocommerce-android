package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.OrdersModule.AddOrderTrackingProviderListFragmentModule
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListFragment
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    AddOrderTrackingProviderListFragmentModule::class
])
object OrdersModule {
    @Module
    abstract class AddOrderTrackingProviderListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddOrderTrackingProviderListModule::class])
        abstract fun addOrderTrackingProviderListFragment(): AddOrderTrackingProviderListFragment
    }
}
