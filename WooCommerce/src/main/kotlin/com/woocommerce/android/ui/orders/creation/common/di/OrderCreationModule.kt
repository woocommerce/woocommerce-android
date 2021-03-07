package com.woocommerce.android.ui.orders.creation.common.di

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.creation.addcustomer.AddCustomerFragment
import com.woocommerce.android.ui.orders.creation.addcustomer.AddCustomerModule
import com.woocommerce.android.ui.orders.creation.neworder.NewOrderFragment
import com.woocommerce.android.ui.orders.creation.neworder.NewOrderModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface OrderCreationModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [NewOrderModule::class])
    fun newOrderFragment(): NewOrderFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [AddCustomerModule::class])
    fun customerCreationFragment(): AddCustomerFragment
}
