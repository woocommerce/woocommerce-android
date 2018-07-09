package com.woocommerce.android.ui.prefs

import com.woocommerce.android.di.FragmentScope

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class PrivacySettingsFragmentModule {
    @FragmentScope
    @Binds
    abstract fun providePrivacySettingsFragmentPresenter(
        privacySettingsFragmentPresenter: PrivacySettingsFragmentPresenter): PrivacySettingsFragmentContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun privacySettingsFragment(): PrivacySettingsFragment
}
