package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module

@Module
internal abstract class AddOrderNoteModule {
    @ActivityScope
    @Binds
    abstract fun provideAddOrderNotePresenter(addOrderNotePresenter: AddOrderNotePresenter):
            AddOrderNoteContract.Presenter

    @ActivityScope
    @Binds
    abstract fun provideUiMessageResolver(uiMessageResolver: AddOrderNoteUIMessageResolver): UIMessageResolver
}
