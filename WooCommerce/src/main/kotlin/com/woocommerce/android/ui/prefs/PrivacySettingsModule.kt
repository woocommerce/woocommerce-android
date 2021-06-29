package com.woocommerce.android.ui.prefs

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped

@InstallIn(FragmentComponent::class)
@Module
internal abstract class PrivacySettingsModule {
    @FragmentScoped
    @Binds
    abstract fun providePrivacySettingsPresenter(privacySettingsPresenter: PrivacySettingsPresenter):
        PrivacySettingsContract.Presenter
}
