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
) {
    private val cached = ConcurrentHashMap<Boolean, TapToPayDeviceSupport>()

    fun checkSupport(): TapToPayDeviceSupport {
        val isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled()
        cached[isSimulated]?.let { return it }
        return when (cardReaderManager.isTapToPaySupportedOnDevice(isSimulated = isSimulated)) {
            TapToPaySupportResult.Supported ->
                TapToPayDeviceSupport.Supported.also { cached[isSimulated] = it }

            is TapToPaySupportResult.NotSupported ->
                TapToPayDeviceSupport.NotSupported.also { cached[isSimulated] = it }

            TapToPaySupportResult.TerminalNotInitialized -> TapToPayDeviceSupport.Unknown
        }
    }
}

sealed class TapToPayDeviceSupport {
    object Supported : TapToPayDeviceSupport()
    object NotSupported : TapToPayDeviceSupport()
    object Unknown : TapToPayDeviceSupport()
}
