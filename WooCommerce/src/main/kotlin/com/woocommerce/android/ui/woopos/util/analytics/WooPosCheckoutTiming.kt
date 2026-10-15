package com.woocommerce.android.ui.woopos.util.analytics

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

class WooPosElapsedTime @Inject constructor() {
    fun milliseconds(): Long = SystemClock.elapsedRealtime()
}

/** References preserve attribution when old asynchronous payment work overlaps a newer checkout. */
@Singleton
class WooPosCheckoutTiming @Inject constructor(private val clock: WooPosElapsedTime) {
    class Context internal constructor(
        internal val checkout: Checkout?,
        internal val payment: Payment,
        internal val strategy: String,
    )

    internal class Checkout(val generation: Long, val started: Long) {
        var sync: Long? = null
    }

    internal class Payment {
        var confirmed: Long? = null
        var closed = false
    }

    private var generation = 0L
    private var current = Context(null, Payment(), "unknown")

    @Synchronized
    fun captureContext(): Context = current

    @Synchronized
    fun startCheckout(syncStrategy: String): Long {
        generation++
        current = Context(Checkout(generation, clock.milliseconds()), Payment(), syncStrategy)
        return generation
    }

    @Synchronized
    fun orderSyncCompleted(checkoutGeneration: Long) {
        current.checkout?.takeIf { it.generation == checkoutGeneration && it.sync == null }?.let {
            it.sync = clock.milliseconds()
        }
    }

    @Synchronized
    fun closeCheckout() {
        generation++
        current = Context(null, Payment(), current.strategy)
    }

    @Synchronized
    fun startPayment(): Context {
        val checkout = current.checkout.takeUnless { current.payment.closed }
        current = Context(checkout, Payment(), current.strategy)
        return current
    }

    @Synchronized
    fun confirmedIntentAvailable(context: Context = current) {
        if (context.checkout != null && !context.payment.closed && context.payment.confirmed == null) {
            context.payment.confirmed = clock.milliseconds()
        }
    }

    @Synchronized
    fun terminalSnapshot(
        success: Boolean,
        includeCardStages: Boolean = true,
        context: Context = current,
    ): Map<String, Any> {
        val properties = mutableMapOf<String, Any>("sync_strategy" to context.strategy)
        val checkout = context.checkout.takeUnless { context.payment.closed }
        val now = checkout?.let { clock.milliseconds() }
        fun add(key: String, stamp: Long?) {
            if (now == null || stamp == null) return
            val elapsed = now - stamp
            if (elapsed >= 0) properties[key] = elapsed
        }
        add("pos_time_since_checkout_started_ms", checkout?.started)
        add("pos_time_since_order_sync_completed_ms", checkout?.sync)
        if (includeCardStages) add("pos_time_since_payment_confirmed_ms", context.payment.confirmed)
        context.payment.confirmed = null
        if (success) context.payment.closed = true
        return properties
    }
}
