package com.woocommerce.android.cardreader.internal

import android.app.Application
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

internal class CardReaderManagerImpl(private val terminal: TerminalWrapper) : CardReaderManager {
    private lateinit var application: Application

    override fun isInitialized(): Boolean {
        return terminal.isInitialized()
    }

    override fun initialize(app: Application) {
        application = app

        // Register the observer for all lifecycle hooks
        app.registerActivityLifecycleCallbacks(terminal.getLifecycleObserver())
    }

    override fun onTrimMemory(level: Int) {
        if (terminal.isInitialized()) {
            terminal.getLifecycleObserver().onTrimMemory(level, application)
        }
    }
}
