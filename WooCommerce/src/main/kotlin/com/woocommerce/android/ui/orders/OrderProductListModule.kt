package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderProductListModule {
    @FragmentScope
    @Binds
    abstract fun provideOrderProductListPresenter(
        orderProductListPresenter: OrderProductListPresenter
    ): OrderProductListContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun orderProductListFragment(): OrderProductListFragment
}
