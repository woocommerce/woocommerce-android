package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class SitePickerModule {
    @ActivityScope
    @Binds
    abstract fun provideSitePickerPresenter(sitePickerPresenter: SitePickerPresenter):
            SitePickerContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun loginEmailHelpDialogFragment(): LoginEmailHelpDialogFragment
}
