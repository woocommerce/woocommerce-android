package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.OrdersModule.AddOrderNoteFragmentModule
import com.woocommerce.android.ui.orders.OrdersModule.AddOrderShipmentTrackingFragmentModule
import com.woocommerce.android.ui.orders.OrdersModule.AddOrderTrackingProviderListFragmentModule
import com.woocommerce.android.ui.orders.OrdersModule.OrderDetailFragmentModule
import com.woocommerce.android.ui.orders.OrdersModule.OrderListFragmentModule
import com.woocommerce.android.ui.orders.details.OrderDetailFragment
import com.woocommerce.android.ui.orders.details.OrderDetailModule
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.orders.list.OrderListModule
import com.woocommerce.android.ui.orders.notes.AddOrderNoteFragment
import com.woocommerce.android.ui.orders.notes.AddOrderNoteModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    OrderListFragmentModule::class,
    OrderDetailFragmentModule::class,
    AddOrderNoteFragmentModule::class,
    AddOrderShipmentTrackingFragmentModule::class,
    AddOrderTrackingProviderListFragmentModule::class
])
object OrdersModule {
    @Module
    abstract class OrderListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [OrderListModule::class])
        abstract fun orderListFragment(): OrderListFragment
    }

    @Module
    abstract class OrderDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [OrderDetailModule::class])
        abstract fun orderDetailFragment(): OrderDetailFragment
    }

    @Module
    abstract class AddOrderNoteFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddOrderNoteModule::class])
        abstract fun addOrderNoteFragment(): AddOrderNoteFragment
    }

    @Module
    abstract class AddOrderShipmentTrackingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddOrderShipmentTrackingModule::class])
        abstract fun addOrderShipmentTrackingFragment(): AddOrderShipmentTrackingFragment
    }

    @Module
    abstract class AddOrderTrackingProviderListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddOrderTrackingProviderListModule::class])
        abstract fun addOrderTrackingProviderListFragment(): AddOrderTrackingProviderListFragment
    }
}
