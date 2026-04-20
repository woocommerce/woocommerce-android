package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import java.util.concurrent.atomic.AtomicReference

class CompositeConnectionTokenProvider internal constructor(
    private val defaultProvider: ConnectionTokenProvider,
) : ConnectionTokenProvider {
    private val active: AtomicReference<ConnectionTokenProvider> = AtomicReference(defaultProvider)

    fun useDefault() {
        active.set(defaultProvider)
    }

    fun use(provider: ConnectionTokenProvider) {
        active.set(provider)
    }

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        active.get().fetchConnectionToken(callback)
    }
}
