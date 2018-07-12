package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class OrderDetailAddNoteModule {
    @ActivityScope
    @Binds
    abstract fun provideOrderDetailAddNotePresenter(orderDetailAddNotePresenter: OrderDetailAddNotePresenter):
            OrderDetailAddNoteContract.Presenter
}
