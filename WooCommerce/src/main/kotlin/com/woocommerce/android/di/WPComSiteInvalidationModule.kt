package com.woocommerce.android.di

import com.woocommerce.android.network.WPComSiteInvalidationNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationListener

@Module
@InstallIn(SingletonComponent::class)
interface WPComSiteInvalidationModule {
    @Binds
    fun bindWPComSiteInvalidationListener(
        notifier: WPComSiteInvalidationNotifier
    ): WPComSiteInvalidationListener
}
