package com.woocommerce.android.ui.orders.creation.common.di

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.creation.common.di.OrderCreationModule.NewOrderFragmentModule
import com.woocommerce.android.ui.orders.creation.neworder.NewOrderFragment
import com.woocommerce.android.ui.orders.creation.neworder.NewOrderModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(
    includes = [
        NewOrderFragmentModule::class
    ]
)
object OrderCreationModule {
    @Module
    internal abstract class NewOrderFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [NewOrderModule::class])
        abstract fun newOrderFragment(): NewOrderFragment
    }
}
