package com.woocommerce.android.ui.dashboard

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class DashboardModule {
    @Binds
    abstract fun provideDashboardPresenter(dashboardPresenter: DashboardPresenter): DashboardContract.Presenter

    @ContributesAndroidInjector
    abstract fun dashboardFragment(): DashboardFragment
}
