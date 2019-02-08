package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.di.ActivityScope
import dagger.Binds
import dagger.Module

@Module
internal abstract class SitePickerModule {
    @ActivityScope
    @Binds
    abstract fun provideSitePickerPresenter(sitePickerPresenter: SitePickerPresenter):
            SitePickerContract.Presenter
}
