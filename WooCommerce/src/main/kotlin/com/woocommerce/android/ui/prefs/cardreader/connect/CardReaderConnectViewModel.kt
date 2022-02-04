package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_LOCATION_FAILURE
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_LOCATION_MISSING_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_LOCATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.Failed
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.Started
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.Succeeded
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.SpecificReader
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateInProgress
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckBluetoothEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckBluetoothPermissionsGiven
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckLocationEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.OpenLocationSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.RequestBluetoothRuntimePermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.RequestEnableBluetooth
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.ShowCardReaderTutorial
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.ShowUpdateInProgress
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.BluetoothDisabledError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.ConnectingFailedState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.ConnectingState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.InvalidMerchantAddressPostCodeError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.LocationDisabledError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.MissingBluetoothPermissionsError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.MissingLocationPermissionsError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.MissingMerchantAddressError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.MultipleReadersFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.ReaderFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.ScanningFailedState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewState.ScanningState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class CardReaderConnectViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val tracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefs,
    private val onboardingChecker: CardReaderOnboardingChecker,
    private val locationRepository: CardReaderLocationRepository,
    private val selectedSite: SelectedSite,
    private val cardReaderManager: CardReaderManager,
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag,
    private val wooStore: WooCommerceStore,
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderConnectDialogFragmentArgs by savedState.navArgs()

    /**
     * This is a workaround for a bug in MultiLiveEvent, which can't be fixed without vital changes.
     * When multiple events are send synchronously to MultiLiveEvent only the first one gets handled
     * as MultiLiveEvent.pending field gets set to false when the first events is handled and all the other events
     * are ignored.
     * Example: Imagine VM sends CheckPermissions event -> the view layer synchronously checks the permissions and
     * invokes vm.permissionChecked(true), the vm sends CheckBluetoothEvent, but this event is never observed by the
     * view layer, since `MultiLiveEvent.pending` was set to false by the previous event.
     * Since this VM doesn't need to have support for MultiLiveEvent, it overrides _event from the parent
     * with SingleLiveEvent.
     */
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    // The app shouldn't store the state as connection flow gets canceled when the vm dies
    private val viewState = MutableLiveData<CardReaderConnectViewState>(ScanningState(::onCancelClicked))
    private var requiredUpdateStarted: Boolean = false
    private var connectionStarted: Boolean = false

    val viewStateData: LiveData<CardReaderConnectViewState> = viewState

    init {
        startFlow()
    }

    private fun startFlow() {
        viewState.value = ScanningState(::onCancelClicked)
        if (arguments.skipOnboarding) {
            triggerEvent(CheckLocationPermissions(::onCheckLocationPermissionsResult))
        } else {
            checkOnboardingState()
        }
    }

    private fun restartFlow() {
        this@CardReaderConnectViewModel.coroutineContext.cancelChildren()
        startFlow()
    }

    fun onScreenStarted() {
        if (viewState.value is MissingLocationPermissionsError || viewState.value is MissingBluetoothPermissionsError) {
            triggerEvent(CheckLocationPermissions(::onCheckLocationPermissionsResult))
        }
    }

    private fun onCheckLocationPermissionsResult(granted: Boolean) {
        if (granted) {
            onLocationPermissionsVerified()
        } else if (viewState.value !is MissingLocationPermissionsError) {
            triggerEvent(RequestLocationPermissions(::onRequestLocationPermissionsResult))
        }
    }

    private fun onRequestLocationPermissionsResult(granted: Boolean) {
        if (granted) {
            onLocationPermissionsVerified()
        } else {
            viewState.value = MissingLocationPermissionsError(
                onPrimaryActionClicked = ::onOpenPermissionsSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onLocationPermissionsVerified() {
        triggerEvent(CheckLocationEnabled(::onCheckLocationEnabledResult))
    }

    private fun onCheckLocationEnabledResult(enabled: Boolean) {
        if (enabled) {
            triggerEvent(CheckBluetoothPermissionsGiven(::onCheckBluetoothPermissionsResult))
        } else {
            viewState.value = LocationDisabledError(
                onPrimaryActionClicked = ::onOpenLocationProviderSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onOpenPermissionsSettingsClicked() {
        triggerEvent(OpenPermissionsSettings)
    }

    private fun onOpenLocationProviderSettingsClicked() {
        triggerEvent(OpenLocationSettings(::onLocationSettingsClosed))
    }

    private fun onLocationSettingsClosed() {
        triggerEvent(CheckLocationEnabled(::onCheckLocationEnabledResult))
    }

    private fun onCheckBluetoothPermissionsResult(enabled: Boolean) {
        if (enabled) {
            triggerEvent(CheckBluetoothEnabled(::onCheckBluetoothResult))
        } else {
            triggerEvent(RequestBluetoothRuntimePermissions(::onBluetoothRuntimePermissionsRequestResult))
        }
    }

    private fun onBluetoothRuntimePermissionsRequestResult(enabled: Boolean) {
        if (enabled) {
            triggerEvent(CheckBluetoothEnabled(::onCheckBluetoothResult))
        } else {
            viewState.value = MissingBluetoothPermissionsError(
                onPrimaryActionClicked = ::onOpenBluetoothPermissionsSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onCheckBluetoothResult(enabled: Boolean) {
        if (enabled) {
            onReadyToStartScanning()
        } else {
            triggerEvent(RequestEnableBluetooth(::onRequestEnableBluetoothResult))
        }
    }

    private fun onRequestEnableBluetoothResult(enabled: Boolean) {
        if (enabled) {
            onReadyToStartScanning()
        } else {
            viewState.value = BluetoothDisabledError(
                onPrimaryActionClicked = ::onOpenBluetoothSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onOpenBluetoothPermissionsSettingsClicked() {
        triggerEvent(OpenPermissionsSettings)
    }

    private fun onOpenBluetoothSettingsClicked() {
        triggerEvent(RequestEnableBluetooth(::onRequestEnableBluetoothResult))
    }

    private fun onReadyToStartScanning() {
        if (!cardReaderManager.initialized) {
            cardReaderManager.initialize()
        }
        launch {
            startScanningIfNotStarted()
        }
    }

    private fun checkOnboardingState() {
        launch {
            when (onboardingChecker.getOnboardingState()) {
                is CardReaderOnboardingState.GenericError,
                is CardReaderOnboardingState.NoConnectionError -> {
                    viewState.value = ScanningFailedState(::restartFlow, ::onCancelClicked)
                }
                is CardReaderOnboardingState.OnboardingCompleted -> {
                    triggerEvent(CheckLocationPermissions(::onCheckLocationPermissionsResult))
                }
                else -> triggerEvent(CardReaderConnectEvent.NavigateToOnboardingFlow)
            }
        }
    }

    private suspend fun startScanningIfNotStarted() {
        launch { listenToConnectionStatus() }
        launch { listenToSoftwareUpdateStatus() }

        if (cardReaderManager.readerStatus.value !is CardReaderStatus.Connecting) {
            cardReaderManager
                .discoverReaders(
                    isSimulated = BuildConfig.USE_SIMULATED_READER,
                    cardReaderTypesToDiscover = CardReaderTypesToDiscover.SpecificReaders(
                        getSupportedReaders()
                    )
                )
                .flowOn(dispatchers.io)
                .collect { discoveryEvent ->
                    handleScanEvent(discoveryEvent)
                }
        }
    }

    private suspend fun getSupportedReaders() =
        if (inPersonPaymentsCanadaFeatureFlag.isEnabled()) {
            cardReaderConfigFactory.getCardReaderConfigFor(
                getStoreCountryCode()
            ).allReaders
        } else {
            cardReaderConfigFactory.getCardReaderConfigFor(
                getStoreCountryCode()
            ).allReaders.filter { specificReader ->
                specificReader != SpecificReader.WisePade3
            }
        }

    private suspend fun listenToConnectionStatus() {
        cardReaderManager.readerStatus.collect { status ->
            when (status) {
                is CardReaderStatus.Connected -> onReaderConnected(status.cardReader)
                CardReaderStatus.NotConnected -> {
                    if (connectionStarted) onReaderConnectionFailed()
                    else Unit
                }
                CardReaderStatus.Connecting -> {
                    connectionStarted = true
                    viewState.value = ConnectingState(::onCancelClicked)
                }
            }.exhaustive
        }
    }

    private suspend fun listenToSoftwareUpdateStatus() {
        cardReaderManager.softwareUpdateStatus.collect { updateStatus ->
            if (updateStatus is SoftwareUpdateInProgress) {
                if (!requiredUpdateStarted) {
                    requiredUpdateStarted = true
                    triggerEvent(ShowUpdateInProgress)
                }
            } else {
                requiredUpdateStarted = false
            }
        }
    }

    private fun handleScanEvent(discoveryEvent: CardReaderDiscoveryEvents) {
        when (discoveryEvent) {
            Started -> {
                if (viewState.value !is ScanningState) {
                    viewState.value = ScanningState(::onCancelClicked)
                }
            }
            is ReadersFound -> {
                tracker.track(
                    AnalyticsTracker.Stat.CARD_READER_DISCOVERY_READER_DISCOVERED,
                    mapOf("reader_count" to discoveryEvent.list.size)
                )
                onReadersFound(discoveryEvent)
            }
            Succeeded -> {
                // noop
            }
            is Failed -> {
                tracker.track(
                    AnalyticsTracker.Stat.CARD_READER_DISCOVERY_FAILED,
                    this.javaClass.simpleName,
                    null,
                    discoveryEvent.msg
                )
                WooLog.e(WooLog.T.CARD_READER, "Scanning failed: ${discoveryEvent.msg}")
                viewState.value = ScanningFailedState(::restartFlow, ::onCancelClicked)
            }
        }
    }

    fun onUpdateReaderResult(updateResult: CardReaderUpdateViewModel.UpdateResult) {
        when (updateResult) {
            CardReaderUpdateViewModel.UpdateResult.FAILED -> {
                triggerEvent(CardReaderConnectEvent.ShowToast(R.string.card_reader_detail_connected_update_failed))
                exitFlow(connected = false)
            }
            CardReaderUpdateViewModel.UpdateResult.SUCCESS -> {
                // noop
            }
        }.exhaustive
    }

    private fun onReadersFound(discoveryEvent: ReadersFound) {
        if (viewState.value is ConnectingState) return
        val availableReaders = discoveryEvent.list.filter { it.id != null }
        val lastKnownReader = findLastKnowReader(availableReaders)
        if (lastKnownReader != null) {
            tracker.track(AnalyticsTracker.Stat.CARD_READER_AUTO_CONNECTION_STARTED)
            connectToReader(lastKnownReader)
        } else {
            viewState.value = when {
                availableReaders.isEmpty() -> ScanningState(::onCancelClicked)
                availableReaders.size == 1 -> buildSingleReaderFoundState(availableReaders[0])
                availableReaders.size > 1 -> buildMultipleReadersFoundState(availableReaders)
                else -> throw IllegalStateException("Unreachable code")
            }
        }
    }

    private fun buildSingleReaderFoundState(reader: CardReader) =
        ReaderFoundState(
            onPrimaryActionClicked = { onConnectToReaderClicked(reader) },
            onSecondaryActionClicked = ::onCancelClicked,
            readerId = reader.id.orEmpty()
        )

    private fun buildMultipleReadersFoundState(availableReaders: List<CardReader>): MultipleReadersFoundState {
        val listItems: MutableList<ListItemViewState> = availableReaders
            .map { mapReaderToListItem(it) }
            .toMutableList()
            .also { it.add(ListItemViewState.ScanningInProgressListItem) }
        return MultipleReadersFoundState(listItems, ::onCancelClicked)
    }

    private fun mapReaderToListItem(reader: CardReader): ListItemViewState =
        ListItemViewState.CardReaderListItem(
            readerId = reader.id.orEmpty(),
            readerType = reader.type,
            onConnectClicked = {
                onConnectToReaderClicked(reader)
            }
        )

    private fun onConnectToReaderClicked(cardReader: CardReader) {
        tracker.track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_TAPPED)
        connectToReader(cardReader)
    }

    private fun connectToReader(cardReader: CardReader) {
        launch {
            val cardReaderLocationId = cardReader.locationId
            if (cardReaderLocationId != null) {
                cardReaderManager.startConnectionToReader(cardReader, cardReaderLocationId)
            } else {
                when (val result = locationRepository.getDefaultLocationId(getPaymentPluginType())) {
                    is CardReaderLocationRepository.LocationIdFetchingResult.Success -> {
                        tracker.track(CARD_READER_LOCATION_SUCCESS)
                        cardReaderManager.startConnectionToReader(cardReader, result.locationId)
                    }
                    is CardReaderLocationRepository.LocationIdFetchingResult.Error -> {
                        handleLocationFetchingError(result)
                    }
                }.exhaustive
            }
        }
    }

    private fun getPaymentPluginType(): PluginType = appPrefs.getPaymentPluginType(
        selectedSite.get().id,
        selectedSite.get().siteId,
        selectedSite.get().selfHostedSiteId
    )

    private fun handleLocationFetchingError(result: CardReaderLocationRepository.LocationIdFetchingResult.Error) {
        this@CardReaderConnectViewModel.coroutineContext.cancelChildren()
        when (result) {
            is CardReaderLocationRepository.LocationIdFetchingResult.Error.MissingAddress -> {
                trackLocationFailureFetching("Missing Address")
                viewState.value = MissingMerchantAddressError(
                    {
                        tracker.track(CARD_READER_LOCATION_MISSING_TAPPED)
                        triggerOpenUrlEventAndExitIfNeeded(result)
                    },
                    {
                        onCancelClicked()
                    }
                )
            }
            is CardReaderLocationRepository.LocationIdFetchingResult.Error.InvalidPostalCode -> {
                trackLocationFailureFetching("Invalid Postal Code")
                viewState.value = InvalidMerchantAddressPostCodeError(::restartFlow)
            }
            is CardReaderLocationRepository.LocationIdFetchingResult.Error.Other -> {
                trackLocationFailureFetching(result.error)
                onReaderConnectionFailed()
            }
        }
    }

    private fun onReaderConnectionFailed() {
        tracker.track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_FAILED)
        WooLog.e(WooLog.T.CARD_READER, "Connecting to reader failed.")
        viewState.value = ConnectingFailedState(::restartFlow, ::onCancelClicked)
    }

    private fun triggerOpenUrlEventAndExitIfNeeded(
        result: CardReaderLocationRepository.LocationIdFetchingResult.Error.MissingAddress
    ) {
        if (selectedSite.getIfExists()?.isWPCom == true || selectedSite.getIfExists()?.isWPComAtomic == true) {
            triggerEvent(CardReaderConnectEvent.OpenWPComWebView(result.url))
        } else {
            triggerEvent(CardReaderConnectEvent.OpenGenericWebView(result.url))
            exitFlow(connected = false)
        }
    }

    private fun onCancelClicked() {
        WooLog.e(WooLog.T.CARD_READER, "Connection flow interrupted by the user.")
        exitFlow(connected = false)
    }

    private fun onReaderConnected(cardReader: CardReader) {
        tracker.track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_SUCCESS)
        WooLog.e(WooLog.T.CARD_READER, "Connecting to reader succeeded.")
        storeConnectedReader(cardReader)

        // show the tutorial if this is the first time the user has connected a reader, otherwise we're done
        if (appPrefs.getShowCardReaderConnectedTutorial()) {
            triggerEvent(ShowCardReaderTutorial)
            appPrefs.setShowCardReaderConnectedTutorial(false)
        } else {
            exitFlow(connected = true)
        }
    }

    fun onTutorialClosed() {
        launch {
            // this workaround needs to be here since the navigation component hasn't finished the previous
            // transaction when a result is received
            delay(1)
            exitFlow(connected = true)
        }
    }

    private fun exitFlow(connected: Boolean) {
        triggerEvent(ExitWithResult(connected))
    }

    private fun storeConnectedReader(cardReader: CardReader) {
        cardReader.id?.let { id -> appPrefs.setLastConnectedCardReaderId(id) }
    }

    private fun findLastKnowReader(readers: List<CardReader>): CardReader? {
        return readers.find { it.id == appPrefs.getLastConnectedCardReaderId() }
    }

    private fun trackLocationFailureFetching(errorDescription: String?) {
        tracker.track(
            CARD_READER_LOCATION_FAILURE,
            this.javaClass.simpleName,
            null,
            errorDescription,
        )
    }

    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
    }

    sealed class ListItemViewState {
        object ScanningInProgressListItem : ListItemViewState() {
            val label = UiStringRes(R.string.card_reader_connect_scanning_progress)

            @DrawableRes
            val scanningIcon = R.drawable.ic_loop_24px
        }

        data class CardReaderListItem(
            val readerId: String,
            val readerType: String?,
            val onConnectClicked: () -> Unit
        ) : ListItemViewState() {
            val connectLabel: UiString = UiStringRes(R.string.card_reader_connect_connect_button)
        }
    }
}
