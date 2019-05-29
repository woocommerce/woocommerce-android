package com.woocommerce.android.ui.orders

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AddOrderNoteModule {
    @Binds
    abstract fun provideAddOrderNotePresenter(addOrderNotePresenter: AddOrderNotePresenter):
            AddOrderNoteContract.Presenter

    @ContributesAndroidInjector
    abstract fun addOrderNoteFragment(): AddOrderNoteFragment
}
