package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Stripe Terminal's device-capability check (`Terminal.supportsReadersOfType`) and caches
 * its answer per reader kind — the device's TTP capability cannot change at runtime, but Stripe
 * answers differently for the simulated and the production reader, and the simulated reader can
 * be toggled from developer options while the process is alive. Only Stripe's answer is cached;
 * the unsupported reason is resolved on every read because it depends on the current date.
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
    private val cached = ConcurrentHashMap<Boolean, TapToPaySupportResult>()

    fun checkSupport(): TapToPayDeviceSupport {
        val isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled()
        val result = cached[isSimulated] ?: cardReaderManager.isTapToPaySupportedOnDevice(isSimulated = isSimulated)
            .also { if (it != TapToPaySupportResult.TerminalNotInitialized) cached[isSimulated] = it }

        return when (result) {
            TapToPaySupportResult.Supported -> TapToPayDeviceSupport.Supported

            is TapToPaySupportResult.NotSupported ->
                TapToPayDeviceSupport.NotSupported(resolveTapToPayUnsupportedReason(result.reason))

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
