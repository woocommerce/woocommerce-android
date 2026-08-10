package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Stripe Terminal's device-capability check (`Terminal.supportsReadersOfType`) and caches
 * the result per reader kind — the device's TTP capability cannot change at runtime, but Stripe
 * answers differently for the simulated and the production reader, and the simulated reader can
 * be toggled from developer options while the process is alive.
 *
 * Returns `null` when Terminal is not yet initialized; callers should treat that as "unknown"
 * and rely on their pre-init heuristics until Stripe can answer.
 */
@Singleton
class TapToPayDeviceSupportChecker @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val appPrefs: AppPrefs,
) {
    private val cached = ConcurrentHashMap<Boolean, Boolean>()

    fun isSupported(): Boolean? {
        val isSimulated = appPrefs.isSimulatedReaderEnabled
        cached[isSimulated]?.let { return it }
        return when (cardReaderManager.isTapToPaySupportedOnDevice(isSimulated = isSimulated)) {
            TapToPaySupportResult.Supported -> true.also { cached[isSimulated] = it }
            is TapToPaySupportResult.NotSupported -> false.also { cached[isSimulated] = it }
            TapToPaySupportResult.TerminalNotInitialized -> null
        }
    }
}
