package com.woocommerce.android.ui.notifications

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class NotifsListModule {
    @Binds
    abstract fun provideNotifsListPresenter(notifsListPresenter: NotifsListPresenter): NotifsListContract.Presenter

    @ContributesAndroidInjector
    abstract fun notifsListFragment(): NotifsListFragment
}
