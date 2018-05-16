package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class DashboardModule {
    @FragmentScope
    @Binds
    abstract fun provideDashboardPresenter(dashboardPresenter: DashboardPresenter): DashboardContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun dashboardFragment(): DashboardFragment
}
