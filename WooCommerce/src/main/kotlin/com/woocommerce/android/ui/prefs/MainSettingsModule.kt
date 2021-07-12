package com.woocommerce.android.ui.prefs

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped

@InstallIn(FragmentComponent::class)
@Module
abstract class MainSettingsModule {
    @FragmentScoped
    @Binds
    abstract fun provideMainSettingsPresenter(mainSettingsPresenter: MainSettingsPresenter):
        MainSettingsContract.Presenter
}
