package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.woocommerce.android.cardreader.LogWrapper
import java.util.concurrent.atomic.AtomicReference

/**
 * Delegates `fetchConnectionToken` to whichever provider is currently active. Lets the Stripe
 * Terminal SDK singleton serve both the existing backend-fetched flows (self-tap TTP, BT reader)
 * and the new Card Reader Mode flow (token supplied over a local TLS socket) without ever being
 * re-initialized.
 *
 * Intended caller lifecycle:
 * - On Card Reader Mode activity `onCreate`: build a `RemoteTokenChannelProvider`, then call
 *   [useRemote].
 * - On Card Reader Mode activity `onDestroy`: call [useDefault] first, then
 *   `remoteProvider.close()`.
 *
 * The atomic swap guarantees a single `fetchConnectionToken` call always routes to whichever
 * provider was active at the moment the SDK read the reference. A call that is already in flight
 * when the mode changes completes on the previous delegate.
 *
 * The public API intentionally only accepts [RemoteTokenChannelProvider] so Stripe SDK types never
 * leak into consumers of this library.
 */
class CompositeConnectionTokenProvider internal constructor(
    private val defaultProvider: ConnectionTokenProvider,
    private val logWrapper: LogWrapper? = null,
) : ConnectionTokenProvider {
    private val active: AtomicReference<ConnectionTokenProvider> = AtomicReference(defaultProvider)

    fun useDefault() {
        active.set(defaultProvider)
    }

    fun useRemote(provider: RemoteTokenChannelProvider) {
        active.set(provider)
    }

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        val current = active.get()
        logWrapper?.d(LOG_TAG, "fetchConnectionToken via ${current::class.java.simpleName}")
        current.fetchConnectionToken(callback)
    }

    private companion object {
        const val LOG_TAG = "CompositeConnectionTokenProvider"
    }
}
