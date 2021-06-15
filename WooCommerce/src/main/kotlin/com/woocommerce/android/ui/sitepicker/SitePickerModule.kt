package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
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

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun loginEmailHelpDialogFragment(): LoginEmailHelpDialogFragment
}
