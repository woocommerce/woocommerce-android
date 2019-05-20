package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module

@Module
internal abstract class AddOrderShipmentTrackingModule {
    @ActivityScope
    @Binds
    abstract fun provideAddOrderShipmentTrackingPresenter(
        addOrderShipmentTrackingPresenter: AddOrderShipmentTrackingPresenter
    ): AddOrderShipmentTrackingContract.Presenter

    @ActivityScope
    @Binds
    abstract fun provideUiMessageResolver(uiMessageResolver: AddShipmentTrackingUIMessageResolver): UIMessageResolver
}
