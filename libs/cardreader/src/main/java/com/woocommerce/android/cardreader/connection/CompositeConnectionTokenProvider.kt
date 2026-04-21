package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import java.util.concurrent.atomic.AtomicReference

/**
 * Delegates `fetchConnectionToken` to whichever provider is currently active. Lets the Stripe
 * Terminal SDK singleton serve both the existing backend-fetched flows (self-tap TTP, BT reader)
 * and the new Card Reader Mode flow (token supplied over a local TLS socket) without ever being
 * re-initialized.
 *
 * Intended caller lifecycle:
 * - On Card Reader Mode activity `onCreate`: build a `RemoteTokenChannelProvider`, then call
 *   [use]`(remoteProvider)`.
 * - On Card Reader Mode activity `onDestroy`: call [useDefault] first, then `remoteProvider.close()`.
 *
 * The atomic swap guarantees a single `fetchConnectionToken` call always routes to whichever
 * provider was active at the moment the SDK read the reference. A call that is already in flight
 * when the mode changes completes on the previous delegate.
 *
 * Treat this as a narrow lifecycle hook for Card Reader Mode. Do not call [use] from any other
 * place — swapping mid-transaction will surface token failures in the existing BT / self-tap flows.
 */
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
