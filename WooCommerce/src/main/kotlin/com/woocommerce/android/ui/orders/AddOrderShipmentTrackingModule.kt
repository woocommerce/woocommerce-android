package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AddOrderShipmentTrackingModule {
    @Binds
    abstract fun provideAddOrderShipmentTrackingPresenter(
        addOrderShipmentTrackingPresenter: AddOrderShipmentTrackingPresenter
    ): AddOrderShipmentTrackingContract.Presenter

    @ContributesAndroidInjector
    abstract fun addOrderShipmentTrackingFragment(): AddOrderShipmentTrackingFragment
}
