package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.MockedOrdersModule.MockedAddOrderShipmentTrackingFragmentModule
import com.woocommerce.android.ui.orders.MockedOrdersModule.MockedAddOrderTrackingProviderListFragmentModule
import com.woocommerce.android.ui.orders.MockedOrdersModule.MockedOrderDetailFragmentModule
import com.woocommerce.android.ui.orders.MockedOrdersModule.MockedOrderFulfillmentFragmentModule
import com.woocommerce.android.ui.orders.MockedOrdersModule.MockedOrderListFragmentModule
import com.woocommerce.android.ui.orders.MockedOrdersModule.MockedOrderProductListFragmentModule
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.products.MockedOrderProductListModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    MockedOrderListFragmentModule::class,
    MockedOrderDetailFragmentModule::class,
    MockedOrderFulfillmentFragmentModule::class,
    MockedAddOrderTrackingProviderListFragmentModule::class,
    MockedAddOrderShipmentTrackingFragmentModule::class,
    MockedOrderProductListFragmentModule::class
])
object MockedOrdersModule {
    @Module
    abstract class MockedOrderListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [MockedOrderListModule::class])
        abstract fun orderListFragment(): OrderListFragment
    }

    @Module
    abstract class MockedOrderDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [MockedOrderDetailModule::class])
        abstract fun orderDetailFragment(): OrderDetailFragment
    }

    @Module
    abstract class MockedOrderProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [MockedOrderProductListModule::class])
        abstract fun orderProductListFragment(): OrderProductListFragment
    }

    @Module
    abstract class MockedOrderFulfillmentFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [MockedOrderFulfillmentModule::class])
        abstract fun orderFulfillmentFragment(): OrderFulfillmentFragment
    }

    @Module
    abstract class MockedAddOrderTrackingProviderListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [MockedAddOrderTrackingProviderListModule::class])
        abstract fun addOrderTrackingProviderListFragment(): AddOrderTrackingProviderListFragment
    }

    @Module
    abstract class MockedAddOrderShipmentTrackingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [MockedAddOrderShipmentTrackingModule::class])
        abstract fun addOrderShipmentTrackingFragment(): AddOrderShipmentTrackingFragment
    }
}
