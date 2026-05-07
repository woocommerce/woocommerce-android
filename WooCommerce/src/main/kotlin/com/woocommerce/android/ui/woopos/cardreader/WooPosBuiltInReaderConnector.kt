package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
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
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosBuiltInReaderConnector @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val locationRepository: CardReaderLocationRepository,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val resourceProvider: ResourceProvider,
    private val fineLocationPermissionCheck: WooPosFineLocationPermissionCheck,
    private val logger: WooPosLogWrapper,
) {
    suspend fun connect(): Result<Unit> {
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected) {
            return Result.success(Unit)
        }

        if (!fineLocationPermissionCheck.isGranted()) {
            logger.e("ACCESS_FINE_LOCATION not granted; built-in reader discovery is blocked")
            return Result.failure(MissingFineLocationPermissionException())
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

        val reader = when (val discovery = discoverFirstBuiltInReader()) {
            is BuiltInDiscoveryResult.Found -> discovery.reader
            is BuiltInDiscoveryResult.Failed -> return Result.failure(
                BuiltInReaderDiscoveryFailedException(discovery.message)
            )
            BuiltInDiscoveryResult.NoReaders -> return Result.failure(
                BuiltInReaderDiscoveryFailedException(message = null)
            )
        }

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

    private sealed interface BuiltInDiscoveryResult {
        data class Found(val reader: CardReader) : BuiltInDiscoveryResult
        data object NoReaders : BuiltInDiscoveryResult
        data class Failed(val message: String?) : BuiltInDiscoveryResult
    }

    private suspend fun discoverFirstBuiltInReader(): BuiltInDiscoveryResult {
        runCatching { initializeCardReaderManager() }.onFailure {
            logger.e("Failed to initialize card reader manager before discovery", it)
            return BuiltInDiscoveryResult.Failed(it.message)
        }
        val event = cardReaderManager
            .discoverReaders(
                isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled(),
                cardReaderTypesToDiscover = BuiltInReaders(listOf(TapToPayDevice)),
            )
            .first { it is CardReaderDiscoveryEvents.ReadersFound || it is CardReaderDiscoveryEvents.Failed }
        return when (event) {
            is CardReaderDiscoveryEvents.ReadersFound ->
                event.list.firstOrNull()?.let(BuiltInDiscoveryResult::Found) ?: BuiltInDiscoveryResult.NoReaders
            is CardReaderDiscoveryEvents.Failed -> BuiltInDiscoveryResult.Failed(event.msg)
            else -> BuiltInDiscoveryResult.Failed(message = null)
        }
    }

    suspend fun disconnectIfConnected() {
        if (cardReaderManager.readerStatus.value !is CardReaderStatus.Connected) return
        runCatching {
            withContext(Dispatchers.Main.immediate) {
                cardReaderManager.disconnectReader()
            }
        }.onFailure { logger.e("Failed to disconnect built-in reader", it) }
    }

    private suspend fun fetchLocationId(): LocationIdFetchingResult {
        val pluginType = cardReaderOnboardingChecker.getOnboardingState().preferredPlugin
            ?: PluginType.WOOCOMMERCE_PAYMENTS
        return locationRepository.getDefaultLocationId(pluginType)
    }

    private suspend fun initializeCardReaderManager() {
        withContext(Dispatchers.Main.immediate) {
            if (!cardReaderManager.initialized) {
                cardReaderManager.initialize(
                    updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
                    useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
                    isDebug = BuildConfig.DEBUG,
                )
                logger.d("Card reader manager initialized for TTP (initialized=${cardReaderManager.initialized})")
            }
            cardReaderManager.setupTapToPayUx(
                CardReaderManager.TapToPayUxConfig(
                    primaryColor = R.color.color_primary,
                    successColor = R.color.woo_green_50,
                    errorColor = R.color.color_error,
                    isDarkMode = resourceProvider.isDarkMode(),
                )
            )
        }
    }
}

internal class MissingFineLocationPermissionException :
    IllegalStateException("ACCESS_FINE_LOCATION permission is required for Tap to Pay")

internal class BuiltInReaderDiscoveryFailedException(message: String?) :
    IllegalStateException(message)
