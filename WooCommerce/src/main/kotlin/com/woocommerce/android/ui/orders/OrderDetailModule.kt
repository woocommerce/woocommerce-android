package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderDetailModule {
    @FragmentScope
    @Binds
    abstract fun provideOrderDetailPresenter(orderDetailPresenter: OrderDetailPresenter): OrderDetailContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun orderDetailfragment(): OrderDetailFragment
}
