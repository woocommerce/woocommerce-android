package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class SelectedSiteModule {
    @Provides
    @Singleton
    fun provideSelectedSite(context: Context, siteStore: SiteStore) = SelectedSite(context, siteStore)
}
