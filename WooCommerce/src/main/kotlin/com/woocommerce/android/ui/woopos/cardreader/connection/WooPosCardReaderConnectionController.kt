package com.woocommerce.android.ui.woopos.cardreader.connection

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders.ExternalReaders
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.Chipper2X
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.StripeM2
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.WisePade3
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository.LocationIdFetchingResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionState.Connected
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosDiscoveredReader
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosUnifiedDiscoveryEvent
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosUnifiedDiscoveryStream
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.LocationUtils
import com.woocommerce.android.util.WooPermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class WooPosCardReaderConnectionController(
    private val cardReaderManager: CardReaderManager,
    private val locationRepository: CardReaderLocationRepository,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val dispatchers: CoroutineDispatchers,
    private val scope: CoroutineScope,
    private val context: Context,
    private val locationUtils: LocationUtils,
    private val logger: WooPosLogWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val tracker: PaymentsFlowTracker,
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
    private val onboardingErrorMapper: WooPosOnboardingErrorMapper,
    private val unifiedDiscoveryStream: WooPosUnifiedDiscoveryStream,
    featureFlagRepository: FeatureFlagRepository,
) {
    private val isRemoteTapToPayEnabled = featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)
    private val _state = MutableStateFlow<WooPosCardReaderConnectionState>(
        WooPosCardReaderConnectionState.Scanning(isRemoteTapToPaySupported = isRemoteTapToPayEnabled)
    )
    val state: StateFlow<WooPosCardReaderConnectionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<ControllerEvent>()
    val event: SharedFlow<ControllerEvent> = _event.asSharedFlow()

    private var connectionFlowJob: Job? = null
    private var discoveryJob: Job? = null
    private var connectionStatusJob: Job? = null
    private var softwareUpdateJob: Job? = null

    private var selectedReader: CardReader? = null
    private var showUpdateCancelWarning = false
    private var isRequiredUpdate = true
    private var isBluetoothPermissionPermanentlyDenied = false
    private var isLocationPermissionPermanentlyDenied = false

    fun showRemoteTapToPayExplainer() {
        if (_state.value !is WooPosCardReaderConnectionState.Scanning) return
        discoveryJob?.cancel()
        _state.value = WooPosCardReaderConnectionState.RemoteTapToPayExplainer(
            onDismissClicked = ::hideRemoteTapToPayExplainer,
        )
    }

    fun hideRemoteTapToPayExplainer() {
        if (_state.value !is WooPosCardReaderConnectionState.RemoteTapToPayExplainer) return
        _state.value = WooPosCardReaderConnectionState.Scanning(
            isRemoteTapToPaySupported = isRemoteTapToPayEnabled,
        )
        startDiscovery()
    }

    private fun enterScanningState() {
        if (_state.value is WooPosCardReaderConnectionState.RemoteTapToPayExplainer) return
        _state.value = WooPosCardReaderConnectionState.Scanning(
            isRemoteTapToPaySupported = isRemoteTapToPayEnabled,
        )
    }

    fun startConnectionFlow() {
        isRequiredUpdate = true
        if (connectionFlowJob?.isActive == true) {
            logger.d("Connection flow already in progress, ignoring")
            return
        }
        enterScanningState()
        connectionFlowJob = scope.launch {
            val onboardingState = cardReaderOnboardingChecker.getOnboardingState()
            if (onboardingState !is CardReaderOnboardingState.OnboardingCompleted) {
                logger.d("Onboarding not completed, state: $onboardingState")
                when (
                    val result = onboardingErrorMapper.map(
                        state = onboardingState,
                        onDismiss = { cancel() },
                        onRetry = { startConnectionFlow() },
                        onSkipPendingRequirements = { continueConnectionFlowAfterOnboarding() },
                    )
                ) {
                    is WooPosOnboardingErrorMapper.Result.DialogError ->
                        _state.value = result.error

                    is WooPosOnboardingErrorMapper.Result.FullScreenRequired ->
                        emitEvent(ControllerEvent.OnboardingRequired(onboardingState))
                }
                return@launch
            }

            continueConnectionFlowAfterOnboarding()
        }
    }

    fun onOnboardingCompleted() {
        if (connectionFlowJob?.isActive == true) {
            logger.d("Connection flow already in progress, ignoring onOnboardingCompleted")
            return
        }
        enterScanningState()
        connectionFlowJob = scope.launch {
            continueConnectionFlowAfterOnboarding()
        }
    }

    private fun continueConnectionFlowAfterOnboarding() {
        initializeCardReaderManagerIfNeeded()
        listenToConnectionStatus()
        listenToSoftwareUpdateStatus()
        checkRequirementsAndStartDiscovery()
    }

    fun startUpdateFlow() {
        isRequiredUpdate = false
        _state.value = WooPosCardReaderConnectionState.UpdateAvailable(
            progress = 0f,
            showCancelWarning = false,
            onCancelClicked = { cancelUpdate() },
            onBackClicked = { onUpdateBackClicked() }
        )
        initializeCardReaderManagerIfNeeded()
        listenToSoftwareUpdateStatus()
        scope.launch {
            cardReaderManager.startAsyncSoftwareUpdate()
        }
    }

    fun onBluetoothPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        isBluetoothPermissionPermanentlyDenied = !granted && !shouldShowRationale
        checkRequirementsAndStartDiscovery()
    }

    fun onLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        isLocationPermissionPermanentlyDenied = !granted && !shouldShowRationale
        checkRequirementsAndStartDiscovery()
    }

    fun onBluetoothEnabled() {
        checkRequirementsAndStartDiscovery()
    }

    fun onLocationEnabled() {
        checkRequirementsAndStartDiscovery()
    }

    fun recheckPermissions() {
        val currentState = _state.value
        if (currentState is WooPosCardReaderConnectionState.MissingBluetoothPermission ||
            currentState is WooPosCardReaderConnectionState.MissingLocationPermission
        ) {
            checkRequirementsAndStartDiscovery()
        }
    }

    @Suppress("DEPRECATION")
    private fun checkRequirementsAndStartDiscovery() {
        when {
            !WooPermissionUtils.hasBluetoothScanPermission(context) ||
                !WooPermissionUtils.hasBluetoothConnectPermission(context) -> {
                logger.d("Bluetooth permission not granted")
                _state.value = WooPosCardReaderConnectionState.MissingBluetoothPermission(
                    onRequestPermissionClicked = {
                        if (isBluetoothPermissionPermanentlyDenied) {
                            emitEvent(ControllerEvent.OpenAppSettings)
                        } else {
                            emitEvent(ControllerEvent.RequestBluetoothPermission)
                        }
                    },
                    onCancelClicked = { cancel() }
                )
            }
            BluetoothAdapter.getDefaultAdapter()?.isEnabled != true -> {
                logger.d("Bluetooth is disabled")
                _state.value = WooPosCardReaderConnectionState.BluetoothDisabled(
                    onEnableBluetoothClicked = { emitEvent(ControllerEvent.RequestEnableBluetooth) },
                    onCancelClicked = { cancel() }
                )
            }
            !WooPermissionUtils.hasFineLocationPermission(context) -> {
                logger.d("Location permission not granted")
                _state.value = WooPosCardReaderConnectionState.MissingLocationPermission(
                    onRequestPermissionClicked = {
                        if (isLocationPermissionPermanentlyDenied) {
                            emitEvent(ControllerEvent.OpenAppSettings)
                        } else {
                            emitEvent(ControllerEvent.RequestLocationPermission)
                        }
                    },
                    onCancelClicked = { cancel() }
                )
            }
            !locationUtils.isLocationEnabled() -> {
                logger.d("Location is disabled")
                _state.value = WooPosCardReaderConnectionState.LocationDisabled(
                    onEnableLocationClicked = { emitEvent(ControllerEvent.RequestEnableLocation) },
                    onCancelClicked = { cancel() }
                )
            }
            else -> {
                startDiscovery()
            }
        }
    }

    private fun emitEvent(event: ControllerEvent) {
        scope.launch {
            _event.emit(event)
        }
    }

    sealed interface ControllerEvent {
        data object RequestBluetoothPermission : ControllerEvent
        data object RequestEnableBluetooth : ControllerEvent
        data object RequestLocationPermission : ControllerEvent
        data object RequestEnableLocation : ControllerEvent
        data object OpenAppSettings : ControllerEvent
        data object Cancelled : ControllerEvent
        data class OnboardingRequired(val onboardingState: CardReaderOnboardingState) : ControllerEvent
    }

    fun cancel() {
        connectionFlowJob?.cancel()
        discoveryJob?.cancel()
        connectionStatusJob?.cancel()
        softwareUpdateJob?.cancel()
        selectedReader = null
        enterScanningState()
        emitEvent(ControllerEvent.Cancelled)
    }

    suspend fun disconnect() {
        appPrefsWrapper.removeLastConnectedCardReaderId()
        cardReaderManager.disconnectReader()
    }

    private fun initializeCardReaderManagerIfNeeded() {
        if (!cardReaderManager.initialized) {
            cardReaderManager.initialize(
                updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
                useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
                isDebug = BuildConfig.DEBUG,
            )
        }
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            unifiedDiscoveryStream
                .discover(
                    isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled(),
                    cardReaderTypesToDiscover = ExternalReaders(
                        listOf(Chipper2X, StripeM2, WisePade3)
                    )
                )
                .flowOn(dispatchers.io)
                .collect { event ->
                    handleDiscoveryEvent(event)
                }
        }
    }

    private fun handleDiscoveryEvent(event: WooPosUnifiedDiscoveryEvent) {
        when (event) {
            is WooPosUnifiedDiscoveryEvent.Started -> {
                logger.d("Discovery started")
                enterScanningState()
            }
            is WooPosUnifiedDiscoveryEvent.ReadersFound -> {
                val bluetoothReaders = event.readers
                    .filterIsInstance<WooPosDiscoveredReader.Bluetooth>()
                    .map { it.cardReader }
                logger.d("Found ${bluetoothReaders.size} readers")
                handleReadersFound(bluetoothReaders)
            }
            is WooPosUnifiedDiscoveryEvent.Failed -> {
                logger.e("Discovery failed - ${event.msg}")
                tracker.trackReaderDiscoveryFailed(event.msg)
                _state.value = WooPosCardReaderConnectionState.ScanningFailed(
                    errorMessage = event.msg,
                    onRetryClicked = { checkRequirementsAndStartDiscovery() },
                    onCancelClicked = { cancel() }
                )
            }
            is WooPosUnifiedDiscoveryEvent.Succeeded -> {
                logger.d("Discovery succeeded")
            }
        }
    }

    private fun handleReadersFound(readers: List<CardReader>) {
        if (_state.value is WooPosCardReaderConnectionState.Connecting) return

        tracker.trackReadersDiscovered(readers.size)

        val lastKnownReader = findLastKnownReader(readers)
        if (lastKnownReader != null) {
            logger.d("Auto-connecting to last known reader: ${lastKnownReader.id}")
            tracker.trackAutoConnectionStarted()
            connectToReader(lastKnownReader)
            return
        }

        when {
            readers.isEmpty() -> {
                enterScanningState()
            }
            readers.size == 1 -> {
                val reader = readers.first()
                _state.value = WooPosCardReaderConnectionState.ReaderFound(
                    reader = WooPosCardReaderConnectionState.FoundReader(
                        id = reader.id ?: "",
                        name = reader.id ?: "Unknown Reader",
                        onConnectClicked = { onConnectToReaderClicked(reader) }
                    ),
                    onKeepSearchingClicked = { continueSearching() },
                )
            }
            else -> {
                _state.value = WooPosCardReaderConnectionState.MultipleReadersFound(
                    readers = readers.map { reader ->
                        WooPosCardReaderConnectionState.FoundReader(
                            id = reader.id ?: "",
                            name = reader.id ?: "Unknown Reader",
                            onConnectClicked = { onConnectToReaderClicked(reader) }
                        )
                    },
                    onCancelClicked = { cancel() }
                )
            }
        }
    }

    private fun findLastKnownReader(readers: List<CardReader>): CardReader? {
        val lastConnectedId = appPrefsWrapper.getLastConnectedCardReaderId()
        return readers.find { it.id == lastConnectedId }
    }

    private fun continueSearching() {
        enterScanningState()
    }

    private fun onConnectToReaderClicked(reader: CardReader) {
        cardReaderTrackingInfoKeeper.setCardReaderModel(reader.type)
        tracker.trackOnConnectTapped()
        connectToReader(reader)
    }

    private fun connectToReader(reader: CardReader) {
        cardReaderTrackingInfoKeeper.setCardReaderModel(reader.type)
        selectedReader = reader
        _state.value = WooPosCardReaderConnectionState.Connecting

        scope.launch {
            when (val locationResult = fetchLocationId()) {
                is LocationIdFetchingResult.Success -> {
                    tracker.trackFetchingLocationSucceeded()
                    cardReaderManager.startConnectionToReader(reader, locationResult.locationId)
                }
                is LocationIdFetchingResult.Error.MissingAddress -> {
                    tracker.trackFetchingLocationFailed("Missing Address")
                    _state.value = WooPosCardReaderConnectionState.InvalidMerchantAddress(
                        onCancelClicked = { cancel() }
                    )
                }
                is LocationIdFetchingResult.Error.InvalidPostalCode -> {
                    tracker.trackFetchingLocationFailed("Invalid Postal Code")
                    _state.value = WooPosCardReaderConnectionState.InvalidPostalCode(
                        onCancelClicked = { cancel() }
                    )
                }
                is LocationIdFetchingResult.Error.Other -> {
                    tracker.trackFetchingLocationFailed(locationResult.error)
                    _state.value = WooPosCardReaderConnectionState.ConnectingFailed(
                        errorMessage = locationResult.error ?: "Unknown error",
                        onRetryClicked = { connectToReader(reader) },
                        onCancelClicked = { cancel() }
                    )
                }
            }
        }
    }

    private suspend fun fetchLocationId(): LocationIdFetchingResult {
        val pluginType = cardReaderOnboardingChecker.getOnboardingState().preferredPlugin
            ?: PluginType.WOOCOMMERCE_PAYMENTS
        return locationRepository.getDefaultLocationId(pluginType)
    }

    private fun listenToConnectionStatus() {
        connectionStatusJob?.cancel()
        connectionStatusJob = scope.launch {
            cardReaderManager.readerStatus.collect { status ->
                handleConnectionStatus(status)
            }
        }
    }

    private fun handleConnectionStatus(status: CardReaderStatus) {
        when (status) {
            is CardReaderStatus.Connected -> {
                logger.d("Reader connected - ${status.cardReader.id}")
                cardReaderTrackingInfoKeeper.setCardReaderBatteryLevel(status.cardReader.currentBatteryLevel)
                tracker.trackConnectionSucceeded()
                status.cardReader.id?.let { appPrefsWrapper.setLastConnectedCardReaderId(it) }
                _state.value = Connected(
                    readerName = status.cardReader.id ?: "Card Reader"
                )
            }
            is CardReaderStatus.Connecting -> {
                logger.d("Connecting to reader")
                _state.value = WooPosCardReaderConnectionState.Connecting
            }
            is CardReaderStatus.NotConnected -> {
                if (_state.value is WooPosCardReaderConnectionState.Connecting) {
                    handleConnectionFailed(status.errorCode, status.errorMessage)
                }
            }

            // We display this status in the floating toolbar
            CardReaderStatus.Reconnecting -> Unit
        }
    }

    private fun handleConnectionFailed(
        errorCode: CardReaderStatus.NotConnected.ErrorCode?,
        errorMessage: String?
    ) {
        logger.e("Connection failed - $errorCode: $errorMessage")
        tracker.trackConnectionFailed()
        when (errorCode) {
            CardReaderStatus.NotConnected.ErrorCode.BATTERY_CRITICALLY_LOW -> {
                _state.value = WooPosCardReaderConnectionState.ConnectingFailedBatteryLow(
                    onCancelClicked = { cancel() }
                )
            }
            CardReaderStatus.NotConnected.ErrorCode.BLUETOOTH_PEER_REMOVED_PAIRING,
            CardReaderStatus.NotConnected.ErrorCode.OTHER,
            null -> {
                _state.value = WooPosCardReaderConnectionState.ConnectingFailed(
                    errorMessage = errorMessage ?: "Connection failed",
                    onRetryClicked = {
                        selectedReader?.let { connectToReader(it) } ?: run {
                            enterScanningState()
                            startDiscovery()
                        }
                    },
                    onCancelClicked = { cancel() }
                )
            }
        }
    }

    private fun listenToSoftwareUpdateStatus() {
        softwareUpdateJob?.cancel()
        softwareUpdateJob = scope.launch {
            cardReaderManager.softwareUpdateStatus.collect { status ->
                handleSoftwareUpdateStatus(status)
            }
        }
    }

    private fun handleSoftwareUpdateStatus(status: SoftwareUpdateStatus) {
        when (status) {
            is SoftwareUpdateStatus.InstallationStarted -> {
                logger.d("Software update started")
                tracker.trackSoftwareUpdateStarted(requiredUpdate = isRequiredUpdate)
                showUpdateCancelWarning = false
                _state.value = WooPosCardReaderConnectionState.UpdateRequired(
                    progress = 0f,
                    showCancelWarning = showUpdateCancelWarning,
                    onCancelClicked = { cancelUpdate() },
                    onBackClicked = { onUpdateBackClicked() }
                )
            }
            is SoftwareUpdateStatus.Installing -> {
                logger.d("Software update progress - ${status.progress}")
                _state.value = WooPosCardReaderConnectionState.UpdateRequired(
                    progress = status.progress,
                    showCancelWarning = showUpdateCancelWarning,
                    onCancelClicked = { cancelUpdate() },
                    onBackClicked = { onUpdateBackClicked() }
                )
            }
            is SoftwareUpdateStatus.Success -> {
                logger.d("Software update completed")
                tracker.trackSoftwareUpdateSucceeded(requiredUpdate = isRequiredUpdate)
                showUpdateCancelWarning = false
                _state.value = WooPosCardReaderConnectionState.UpdateCompleted
            }
            is SoftwareUpdateStatus.Failed -> {
                showUpdateCancelWarning = false
                handleSoftwareUpdateFailed(status)
            }
            is SoftwareUpdateStatus.Unknown -> {
                showUpdateCancelWarning = false
                logger.d("Software update status unknown")
            }
        }
    }

    private fun onUpdateBackClicked() {
        showUpdateCancelWarning = true
        when (val currentState = _state.value) {
            is WooPosCardReaderConnectionState.UpdateRequired -> {
                _state.value = currentState.copy(showCancelWarning = true)
            }
            is WooPosCardReaderConnectionState.UpdateAvailable -> {
                _state.value = currentState.copy(showCancelWarning = true)
            }
            else -> error("Invalid state $currentState for update back button clicked")
        }
    }

    private fun cancelUpdate() {
        tracker.trackSoftwareUpdateCancelled(requiredUpdate = isRequiredUpdate)
        cardReaderManager.cancelOngoingFirmwareUpdate()
        cancel()
    }

    private fun handleSoftwareUpdateFailed(status: SoftwareUpdateStatus.Failed) {
        logger.e("Software update failed - ${status.errorType}: ${status.message}")
        tracker.trackSoftwareUpdateFailed(status, requiredUpdate = isRequiredUpdate)
        when (val errorType = status.errorType) {
            is SoftwareUpdateStatusErrorType.BatteryLow -> {
                _state.value = WooPosCardReaderConnectionState.UpdateFailedBatteryLow(
                    currentBatteryLevel = errorType.currentBatteryLevel,
                    onCancelClicked = { cancel() }
                )
            }
            SoftwareUpdateStatusErrorType.Interrupted,
            SoftwareUpdateStatusErrorType.ReaderError,
            SoftwareUpdateStatusErrorType.ServerError,
            SoftwareUpdateStatusErrorType.Failed -> {
                _state.value = WooPosCardReaderConnectionState.UpdateFailed(
                    errorMessage = status.message ?: "Update failed",
                    onRetryClicked = {
                        scope.launch { cardReaderManager.startAsyncSoftwareUpdate() }
                    },
                    onCancelClicked = { cancel() }
                )
            }
        }
    }
}
