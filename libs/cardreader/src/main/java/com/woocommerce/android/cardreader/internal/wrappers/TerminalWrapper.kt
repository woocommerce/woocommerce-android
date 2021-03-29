package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.TerminalLifecycleObserver
import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.log.LogLevel
import com.stripe.stripeterminal.model.external.DiscoveryConfiguration
import com.woocommerce.android.cardreader.internal.TokenProvider

/**
 * Injectable wrapper for Stripe's Terminal object.
 */
internal class TerminalWrapper {
    fun isInitialized() = Terminal.isInitialized()
    fun getLifecycleObserver() = TerminalLifecycleObserver.getInstance()
    fun initTerminal(
        application: Application,
        logLevel: LogLevel,
        tokenProvider: TokenProvider,
        listener: TerminalListener
    ) = Terminal.initTerminal(application, logLevel, tokenProvider, listener)

    fun discoverReaders(
        config: DiscoveryConfiguration,
        discoveryListener: DiscoveryListener,
        callback: Callback
    ): Cancelable = Terminal.getInstance().discoverReaders(config, discoveryListener, callback)
}
