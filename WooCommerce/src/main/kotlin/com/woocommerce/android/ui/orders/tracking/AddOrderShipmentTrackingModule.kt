package com.woocommerce.android.ui.orders.tracking

import dagger.Binds
import dagger.Module

@Module
internal abstract class AddOrderShipmentTrackingModule {
    @Binds
    abstract fun provideAddOrderShipmentTrackingPresenter(
        addOrderShipmentTrackingPresenter: AddOrderShipmentTrackingPresenter
    ): AddOrderShipmentTrackingContract.Presenter
}
