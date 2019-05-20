package com.woocommerce.android.ui.prefs

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class PrivacySettingsModule {
    @Binds
    abstract fun providePrivacySettingsPresenter(privacySettingsPresenter: PrivacySettingsPresenter):
            PrivacySettingsContract.Presenter

    @ContributesAndroidInjector
    abstract fun privacySettingsFragment(): PrivacySettingsFragment
}
