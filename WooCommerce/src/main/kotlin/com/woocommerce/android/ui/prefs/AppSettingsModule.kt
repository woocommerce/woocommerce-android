package com.woocommerce.android.ui.prefs

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
abstract class AppSettingsModule {
    @ActivityScoped
    @Binds
    abstract fun provideAppSettingsPresenter(appSettingsPresenter: AppSettingsPresenter):
        AppSettingsContract.Presenter
}
