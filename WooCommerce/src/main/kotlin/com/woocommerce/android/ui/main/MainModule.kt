package com.woocommerce.android.ui.main

import com.woocommerce.android.di.ActivityScoped
import dagger.Binds
import dagger.Module

@Module
internal abstract class MainModule {
    @ActivityScoped
    @Binds
    abstract fun provideMainPresenter(mainActivityPresenter: MainPresenter): MainContract.Presenter
}
