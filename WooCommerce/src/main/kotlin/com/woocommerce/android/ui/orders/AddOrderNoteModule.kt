package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class AddOrderNoteModule {
    @ActivityScope
    @Binds
    abstract fun provideAddOrderNotePresenter(addOrderNotePresenter: AddOrderNotePresenter):
            AddOrderNoteContract.Presenter
}
