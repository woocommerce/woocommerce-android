package com.woocommerce.android.ui.prefs

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@InstallIn(ActivityRetainedComponent::class)
@Module
abstract class AppSettingsModule {
    @ActivityRetainedScoped
    @Binds
    abstract fun provideAppSettingsPresenter(appSettingsPresenter: AppSettingsPresenter): AppSettingsContract.Presenter
}
