package com.woocommerce.android.ui.main

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class MainModule {
    @ActivityScope
    @Binds
    abstract fun provideMainPresenter(mainActivityPresenter: MainPresenter): MainContract.Presenter

    @ActivityScope
    @Binds
    abstract fun provideMainErrorHandler(mainErrorHandler: MainErrorHandler): MainContract.ErrorHandler
}
