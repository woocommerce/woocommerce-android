package com.woocommerce.android.ui.login

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class MagicLinkInterceptModule {
    @Binds
    abstract fun provideMainPresenter(magicLinkInterceptPresenter: MagicLinkInterceptPresenter):
            MagicLinkInterceptContract.Presenter

    @ContributesAndroidInjector
    internal abstract fun magicLinkInterceptFragment(): MagicLinkInterceptFragment
}
