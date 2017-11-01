package com.woocommerce.android.ui.main

import dagger.Binds
import dagger.Module

@Module
internal abstract class MainModule {
    @Binds
    abstract fun provideMainPresenter(mainActivityPresenter: MainPresenter): MainContract.Presenter
}
