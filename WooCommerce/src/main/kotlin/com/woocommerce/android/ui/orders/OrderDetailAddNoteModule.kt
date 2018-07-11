package com.woocommerce.android.ui.orders

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class OrderDetailAddNoteModule {
    @ActivityScope
    @Binds
    abstract fun provideOrderDetailAddNotePresenter(orderDetailAddNotePresenter: OrderDetailAddNotePresenter):
            OrderDetailAddNoteContract.Presenter
}
