package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module

@Module
internal abstract class AddOrderTrackingProviderListModule {
    @Binds
    abstract fun provideAddOrderTrackingProviderListPresenter(
        addOrderTrackingProviderListPresenter: AddOrderTrackingProviderListPresenter
    ): AddOrderTrackingProviderListContract.Presenter
}
