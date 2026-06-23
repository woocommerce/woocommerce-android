package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Stripe Terminal's device-capability check (`Terminal.supportsReadersOfType`) and caches
 * the result for the lifetime of the process — the device's TTP capability cannot change at
 * runtime, so a single lookup is enough.
 *
 * Returns `null` when Terminal is not yet initialized; callers should treat that as "unknown"
 * and rely on their pre-init heuristics until Stripe can answer.
 */
@Singleton
class TapToPayDeviceSupportChecker @Inject constructor(
    private val cardReaderManager: CardReaderManager,
) {
    @Volatile
    private var cached: Boolean? = null

    fun isSupported(): Boolean? {
        cached?.let { return it }
        return when (cardReaderManager.isTapToPaySupportedOnDevice(isSimulated = false)) {
            TapToPaySupportResult.Supported -> true.also { cached = it }
            is TapToPaySupportResult.NotSupported -> false.also { cached = it }
            TapToPaySupportResult.TerminalNotInitialized -> null
        }
    }
}
