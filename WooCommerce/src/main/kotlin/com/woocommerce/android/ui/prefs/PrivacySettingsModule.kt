package com.woocommerce.android.ui.prefs

import com.woocommerce.android.di.FragmentScope

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class PrivacySettingsModule {
    @FragmentScope
    @Binds
    abstract fun providePrivacySettingsPresenter(privacySettingsPresenter: PrivacySettingsPresenter):
            PrivacySettingsContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun privacySettingsFragment(): PrivacySettingsFragment
}
