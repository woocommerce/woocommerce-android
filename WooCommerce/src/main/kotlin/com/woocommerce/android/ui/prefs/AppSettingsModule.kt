package com.woocommerce.android.ui.prefs

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class AppSettingsModule {
    @ActivityScope
    @Binds
    abstract fun provideAppSettingsPresenter(appSettingsPresenter: AppSettingsPresenter):
            AppSettingsContract.Presenter
}
