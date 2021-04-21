package com.woocommerce.android.ui.main

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
internal abstract class MainModule {
    @ActivityScoped
    @Binds
    abstract fun provideMainPresenter(mainActivityPresenter: MainPresenter): MainContract.Presenter

    @ActivityScoped
    @Binds
    abstract fun provideUiMessageResolver(mainUIMessageResolver: MainUIMessageResolver): UIMessageResolver
}
