package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import com.stripe.stripeterminal.TerminalApplicationDelegate

internal class TerminalApplicationDelegateWrapper {
    private val delegate = TerminalApplicationDelegate

    fun onCreate(application: Application) = delegate.onCreate(application)
}
