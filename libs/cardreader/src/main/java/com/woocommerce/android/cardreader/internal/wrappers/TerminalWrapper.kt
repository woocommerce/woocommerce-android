package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.TerminalLifecycleObserver
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.internal.TokenProvider

internal class TerminalWrapper {
    fun isInitialized() = Terminal.isInitialized()
    fun getLifecycleObserver() = TerminalLifecycleObserver.getInstance()
    fun initTerminal(
        application: Application,
        logLevel: LogLevel,
        tokenProvider: TokenProvider,
        listener: TerminalListener
    ) = Terminal.initTerminal(application, logLevel, tokenProvider, listener)
}
