package com.woocommerce.android.di

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.InvalidSignatureListener

/**
 * Declares [InvalidSignatureListener] as an optional binding so that
 * [org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork] can inject `Optional<InvalidSignatureListener>`
 * in every Hilt graph that uses it (main app, Wear app, feature test graphs, etc.). The concrete implementation
 * is bound by the main app only; other consumers get an empty Optional.
 *
 * Lives in `commons` (a shared dependency of all those graphs) and uses `@InstallIn` so Hilt auto-installs it
 * everywhere, avoiding per-graph wiring.
 */
@Module
@InstallIn(SingletonComponent::class)
interface InvalidSignatureListenerModule {
    @BindsOptionalOf
    fun bindOptionalInvalidSignatureListener(): InvalidSignatureListener
}
