package com.woocommerce.android.ui.prefs

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class MainSettingsModule {
    @Binds
    abstract fun provideMainSettingsPresenter(mainSettingsPresenter: MainSettingsPresenter):
            MainSettingsContract.Presenter

    @ContributesAndroidInjector
    abstract fun mainSettingsFragment(): MainSettingsFragment
}
