package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderFulfillmentModule {
    @FragmentScope
    @Binds
    abstract fun provideOrderFulfillmentPresenter(presenter: OrderFulfillmentPresenter):
            OrderFulfillmentContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun orderFulfillmentFragment(): OrderFulfillmentFragment
}
