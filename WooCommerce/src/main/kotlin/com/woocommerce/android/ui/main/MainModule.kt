package com.woocommerce.android.ui.main

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
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

    @ActivityScope
    @Binds
    abstract fun provideUIMessageResolver(mainUIMessageResolver: MainUIMessageResolver): UIMessageResolver
}
