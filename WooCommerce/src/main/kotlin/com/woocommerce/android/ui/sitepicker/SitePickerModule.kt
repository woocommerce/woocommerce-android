package com.woocommerce.android.ui.sitepicker

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
internal abstract class SitePickerModule {
    @ActivityScoped
    @Binds
    abstract fun provideSitePickerPresenter(sitePickerPresenter: SitePickerPresenter):
        SitePickerContract.Presenter
}
