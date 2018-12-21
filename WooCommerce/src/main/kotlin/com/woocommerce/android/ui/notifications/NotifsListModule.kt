package com.woocommerce.android.ui.notifications

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class NotifsListModule {
    @FragmentScope
    @Binds
    abstract fun provideNotifsListPresenter(notifsListPresenter: NotifsListPresenter): NotifsListContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun notifsListFragment(): NotifsListFragment
}
