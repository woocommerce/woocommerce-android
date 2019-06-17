package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AddOrderTrackingProviderListModule {
    @Binds
    abstract fun provideAddOrderTrackingProviderListPresenter(
        addOrderTrackingProviderListPresenter: AddOrderTrackingProviderListPresenter
    ): AddOrderTrackingProviderListContract.Presenter

    @ContributesAndroidInjector
    abstract fun addOrderTrackingProviderListFragment(): AddOrderTrackingProviderListFragment
}
