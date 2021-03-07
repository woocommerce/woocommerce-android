package com.woocommerce.android.ui.orders.creation.common.di

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.creation.customercreation.CustomerCreationFragment
import com.woocommerce.android.ui.orders.creation.customercreation.CustomerCreationModule
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
    @ContributesAndroidInjector(modules = [CustomerCreationModule::class])
    fun customerCreationFragment(): CustomerCreationFragment
}
