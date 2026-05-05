package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders.BuiltInReaders
import com.woocommerce.android.cardreader.connection.ReaderType.BuildInReader.TapToPayDevice
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository.LocationIdFetchingResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosBuiltInReaderConnector @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val locationRepository: CardReaderLocationRepository,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val logger: WooPosLogWrapper,
) {
    suspend fun connect(): Result<Unit> {
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected) {
            return Result.success(Unit)
        }

        runCatching { initializeCardReaderManager() }.onFailure {
            logger.e("Failed to initialize card reader manager for TTP", it)
            return Result.failure(it)
        }

        val locationId = when (val locationResult = fetchLocationId()) {
            is LocationIdFetchingResult.Success -> locationResult.locationId
            else -> {
                logger.e("Failed to fetch location id: $locationResult")
                return Result.failure(IllegalStateException("Could not fetch store location"))
            }
        }

        val reader = discoverFirstBuiltInReader() ?: return Result.failure(
            IllegalStateException("No built-in reader available")
        )

        cardReaderManager.startConnectionToReader(reader, locationId)

        val terminalStatus = cardReaderManager.readerStatus.first {
            it is CardReaderStatus.Connected || it is CardReaderStatus.NotConnected
        }
        return if (terminalStatus is CardReaderStatus.Connected) {
            Result.success(Unit)
        } else {
            logger.e("Built-in reader connection ended in $terminalStatus")
            Result.failure(IllegalStateException("Built-in reader failed to connect"))
        }
    }

    private suspend fun discoverFirstBuiltInReader(): CardReader? {
        runCatching { initializeCardReaderManager() }.onFailure {
            logger.e("Failed to initialize card reader manager before discovery", it)
            return null
        }
        val event = cardReaderManager
            .discoverReaders(
                isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled(),
                cardReaderTypesToDiscover = BuiltInReaders(listOf(TapToPayDevice)),
            )
            .first { it is CardReaderDiscoveryEvents.ReadersFound || it is CardReaderDiscoveryEvents.Failed }
        return when (event) {
            is CardReaderDiscoveryEvents.ReadersFound -> event.list.firstOrNull()
            else -> null
        }
    }

    private suspend fun fetchLocationId(): LocationIdFetchingResult {
        val pluginType = cardReaderOnboardingChecker.getOnboardingState().preferredPlugin
            ?: PluginType.WOOCOMMERCE_PAYMENTS
        return locationRepository.getDefaultLocationId(pluginType)
    }

    private suspend fun initializeCardReaderManager() {
        if (cardReaderManager.initialized) return
        withContext(Dispatchers.Main.immediate) {
            if (!cardReaderManager.initialized) {
                cardReaderManager.initialize(
                    updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
                    useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
                    isDebug = BuildConfig.DEBUG,
                )
                logger.d("Card reader manager initialized for TTP (initialized=${cardReaderManager.initialized})")
            }
        }
    }
}
