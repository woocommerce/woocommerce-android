package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.TerminalLifecycleObserver

internal class TerminalWrapper {
    fun isInitialized() = Terminal.isInitialized()
    fun getLifecycleObserver() = TerminalLifecycleObserver.getInstance()
}
