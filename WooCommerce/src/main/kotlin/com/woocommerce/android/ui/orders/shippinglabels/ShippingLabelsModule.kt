package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.CreateShippingLabelFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.PrintShippingLabelFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.ShippingLabelRefundFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ShippingLabelRefundFragmentModule::class,
    PrintShippingLabelFragmentModule::class,
    CreateShippingLabelFragmentModule::class
])
object ShippingLabelsModule {
    @Module
    abstract class ShippingLabelRefundFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ShippingLabelRefundModule::class])
        abstract fun shippingLabelRefundFragment(): ShippingLabelRefundFragment
    }
    @Module
    abstract class PrintShippingLabelFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [PrintShippingLabelModule::class])
        abstract fun printShippingLabelFragment(): PrintShippingLabelFragment
    }
    @Module
    abstract class CreateShippingLabelFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [CreateShippingLabelModule::class])
        abstract fun createShippingLabelFragment(): CreateShippingLabelFragment
    }
}
