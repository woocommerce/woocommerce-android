package com.woocommerce.android.ui.orders.notes

import com.woocommerce.android.ui.orders.notes.AddOrderNoteContract.Presenter
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AddOrderNoteModule {
    @Binds
    abstract fun provideAddOrderNotePresenter(addOrderNotePresenter: AddOrderNotePresenter):
            Presenter

    @ContributesAndroidInjector
    abstract fun addOrderNoteFragment(): AddOrderNoteFragment
}
