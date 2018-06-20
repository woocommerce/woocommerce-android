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
    abstract fun provideUiMessageResolver(mainUIMessageResolver: MainUIMessageResolver): UIMessageResolver
}
