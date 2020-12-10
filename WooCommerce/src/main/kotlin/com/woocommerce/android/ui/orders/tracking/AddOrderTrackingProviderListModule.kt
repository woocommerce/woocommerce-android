package com.woocommerce.android.ui.orders.tracking

import dagger.Binds
import dagger.Module

@Module
internal abstract class AddOrderTrackingProviderListModule {
    @Binds
    abstract fun provideAddOrderTrackingProviderListPresenter(
        addOrderTrackingProviderListPresenter: AddOrderTrackingProviderListPresenter
    ): AddOrderTrackingProviderListContract.Presenter
}
