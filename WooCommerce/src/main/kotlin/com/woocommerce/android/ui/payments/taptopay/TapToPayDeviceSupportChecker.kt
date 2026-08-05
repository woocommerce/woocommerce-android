package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Stripe Terminal's device-capability check (`Terminal.supportsReadersOfType`) and caches
 * the result per reader kind — the device's TTP capability cannot change at runtime, but Stripe
 * answers differently for the simulated and the production reader, and the simulated reader can
 * be toggled from developer options while the process is alive.
 *
 * Stripe can only answer once Terminal is initialized, which happens lazily when a card reader
 * flow is entered, so [TapToPayDeviceSupport.Unknown] is the normal answer on a cold start.
 */
@Singleton
class TapToPayDeviceSupportChecker @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val resolveTapToPayUnsupportedReason: ResolveTapToPayUnsupportedReason,
) {
    private val cached = ConcurrentHashMap<Boolean, TapToPayDeviceSupport>()

    fun checkSupport(): TapToPayDeviceSupport {
        val isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled()
        cached[isSimulated]?.let { return it }
        return when (val result = cardReaderManager.isTapToPaySupportedOnDevice(isSimulated = isSimulated)) {
            TapToPaySupportResult.Supported ->
                TapToPayDeviceSupport.Supported.also { cached[isSimulated] = it }

            is TapToPaySupportResult.NotSupported ->
                TapToPayDeviceSupport.NotSupported(resolveTapToPayUnsupportedReason(result.reason))
                    .also { cached[isSimulated] = it }

            TapToPaySupportResult.TerminalNotInitialized -> TapToPayDeviceSupport.Unknown
        }
    }
}

sealed class TapToPayDeviceSupport {
    object Supported : TapToPayDeviceSupport()
    data class NotSupported(val reason: TapToPayUnsupportedReason) : TapToPayDeviceSupport()

    /**
     * Stripe Terminal has not been initialized yet, so the device's capability is not known. It is
     * not an answer of its own — see [TapToPayAvailabilityStatus.Result.Unknown] for how the UI
     * treats it.
     */
    object Unknown : TapToPayDeviceSupport()
}

sealed class TapToPayUnsupportedReason {
    /**
     * The device's Android security patch is older than the 12 months Stripe requires, which the
     * merchant can fix by updating the device.
     */
    object OutdatedSecurityPatch : TapToPayUnsupportedReason()

    /**
     * Stripe rejected the device and the cause could not be narrowed down. [message] is Stripe's
     * own text, which is written for developers rather than merchants.
     */
    data class Unspecified(val message: String) : TapToPayUnsupportedReason()
}
