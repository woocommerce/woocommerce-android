package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
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
    object Unknown : TapToPayDeviceSupport()
}

sealed class TapToPayUnsupportedReason {
    object OutdatedSecurityPatch : TapToPayUnsupportedReason()
    data class Unspecified(val message: String) : TapToPayUnsupportedReason()
}
