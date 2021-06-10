package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.MyStoreContract.Presenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped

@Module
@InstallIn(FragmentComponent::class)
internal abstract class MyStoreModule {
    @Binds
    @FragmentScoped
    abstract fun provideMyStorePresenter(dashboardPresenter: MyStorePresenter): Presenter
}
