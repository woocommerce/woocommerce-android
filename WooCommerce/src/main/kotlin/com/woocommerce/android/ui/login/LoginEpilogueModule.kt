package com.woocommerce.android.ui.login

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class LoginEpilogueModule {
    @ActivityScope
    @Binds
    abstract fun provideLoginEpiloguePresenter(
        loginEpiloguePresenter: LoginEpiloguePresenter): LoginEpilogueContract.Presenter
}
