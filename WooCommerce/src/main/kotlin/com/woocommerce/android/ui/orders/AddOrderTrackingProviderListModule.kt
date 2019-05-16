package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AddOrderTrackingProviderListModule {
    @FragmentScope
    @Binds
    abstract fun provideAddOrderTrackingProviderListPresenter(
        addOrderTrackingProviderListPresenter: AddOrderTrackingProviderListPresenter
    ): AddOrderTrackingProviderListContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun addOrderTrackingProviderListFragment(): AddOrderTrackingProviderListFragment
}
