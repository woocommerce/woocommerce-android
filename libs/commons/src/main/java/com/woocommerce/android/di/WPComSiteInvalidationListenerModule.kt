package com.woocommerce.android.di

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationListener

/**
 * Lives in `commons` so the optional binding is installed in every Hilt graph that injects
 * `Optional<WPComSiteInvalidationListener>`. The concrete implementation is bound by the main app
 * only; other consumers (e.g. the Wear app) get an empty Optional.
 */
@Module
@InstallIn(SingletonComponent::class)
interface WPComSiteInvalidationListenerModule {
    @BindsOptionalOf
    fun bindOptionalWPComSiteInvalidationListener(): WPComSiteInvalidationListener
}
