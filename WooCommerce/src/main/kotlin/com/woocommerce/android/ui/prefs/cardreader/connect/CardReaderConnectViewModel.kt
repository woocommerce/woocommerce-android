package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Failed
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Started
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Succeeded
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckBluetoothEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenLocationSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestEnableBluetooth
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.BluetoothDisabledError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ConnectingState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.LocationDisabledError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.MissingPermissionsError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ReaderFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ScanningState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

@HiltViewModel
class CardReaderConnectViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val appLogWrapper: AppLogWrapper
) : ScopedViewModel(savedState) {
    private lateinit var cardReaderManager: CardReaderManager

    // The app shouldn't store the state as connection flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(ScanningState(::onCancelClicked))
    val viewStateData: LiveData<ViewState> = viewState

    init {
        triggerEvent(CheckLocationPermissions(::onCheckLocationPermissionsResult))
    }

    private fun onCheckLocationPermissionsResult(granted: Boolean) {
        if (granted) {
            onLocationPermissionsVerified()
        } else if (viewState.value !is MissingPermissionsError) {
            triggerEvent(RequestLocationPermissions(::onRequestLocationPermissionsResult))
        }
    }

    private fun onRequestLocationPermissionsResult(granted: Boolean) {
        if (granted) {
            onLocationPermissionsVerified()
        } else {
            viewState.value = MissingPermissionsError(
                onPrimaryActionClicked = ::onOpenPermissionsSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onCheckLocationEnabledResult(enabled: Boolean) {
        if (enabled) {
            onLocationStateVerified()
        } else {
            viewState.value = LocationDisabledError(
                onPrimaryActionClicked = ::onOpenLocationProviderSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onLocationSettingsClosed() {
        triggerEvent(CheckLocationEnabled(::onCheckLocationEnabledResult))
    }

    private fun onCheckBluetoothResult(enabled: Boolean) {
        if (enabled) {
            onBluetoothStateVerified()
        } else {
            triggerEvent(RequestEnableBluetooth(::onRequestEnableBluetoothResult))
        }
    }

    private fun onRequestEnableBluetoothResult(enabled: Boolean) {
        if (enabled) {
            onBluetoothStateVerified()
        } else {
            viewState.value = BluetoothDisabledError(
                onPrimaryActionClicked = ::onOpenBluetoothSettingsClicked,
                onSecondaryActionClicked = ::onCancelClicked
            )
        }
    }

    private fun onCardReaderManagerInitialized(cardReaderManager: CardReaderManager) {
        this.cardReaderManager = cardReaderManager
        // TODO cardreader check location permissions
        launch {
            startScanning()
        }
    }

    private fun onLocationPermissionsVerified() {
        triggerEvent(CheckLocationEnabled(::onCheckLocationEnabledResult))
    }

    private fun onLocationStateVerified() {
        triggerEvent(CheckBluetoothEnabled(::onCheckBluetoothResult))
    }

    private fun onBluetoothStateVerified() {
        triggerEvent(InitializeCardReaderManager(::onCardReaderManagerInitialized))
    }

    private suspend fun startScanning() {
        cardReaderManager
            // TODO cardreader set isSimulated to false or add a temporary checkbox to the UI
            .discoverReaders(isSimulated = true)
            // TODO cardreader should we move flowOn to CardReaderModule?
            .flowOn(dispatchers.io)
            .collect { discoveryEvent ->
                handleScanEvent(discoveryEvent)
            }
    }

    private fun handleScanEvent(discoveryEvent: CardReaderDiscoveryEvents) {
        when (discoveryEvent) {
            Started -> {
                if (viewState.value !is ScanningState) {
                    viewState.value = ScanningState(::onCancelClicked)
                }
            }
            is ReadersFound -> onReadersFound(discoveryEvent)
            Succeeded -> {
                // noop
            }
            is Failed -> {
                // TODO cardreader Replace with failed state
                appLogWrapper.e(T.MAIN, "Scanning failed.")
                triggerEvent(Exit)
            }
        }
    }

    private fun onReadersFound(discoveryEvent: ReadersFound) {
        if (viewState.value is ConnectingState) return
        val availableReaders = discoveryEvent.list.filter { it.getId() != null }
        if (availableReaders.isNotEmpty()) {
            // TODO cardreader add support for showing multiple readers
            val reader = availableReaders[0]
            viewState.value = ReaderFoundState(
                onPrimaryActionClicked = { onConnectToReaderClicked(reader) },
                onSecondaryActionClicked = ::onCancelClicked,
                readerId = reader.getId().orEmpty()
            )
        } else {
            viewState.value = ScanningState(::onCancelClicked)
        }
    }

    private fun onConnectToReaderClicked(cardReader: CardReader) {
        viewState.value = ConnectingState(::onCancelClicked)
        launch {
            val success = cardReaderManager.connectToReader(cardReader)
            if (success) {
                onReaderConnected()
            } else {
                // TODO cardreader Replace with failed state
                appLogWrapper.e(T.MAIN, "Connecting to reader failed.")
                triggerEvent(Exit)
            }
        }
    }

    private fun onOpenPermissionsSettingsClicked() {
        triggerEvent(OpenPermissionsSettings)
    }

    private fun onOpenLocationProviderSettingsClicked() {
        triggerEvent(OpenLocationSettings(::onLocationSettingsClosed))
    }

    private fun onOpenBluetoothSettingsClicked() {
        triggerEvent(RequestEnableBluetooth(::onRequestEnableBluetoothResult))
    }

    private fun onCancelClicked() {
        appLogWrapper.e(T.MAIN, "Connection flow interrupted by the user.")
        triggerEvent(Exit)
    }

    private fun onReaderConnected() {
        appLogWrapper.e(T.MAIN, "Connecting to reader succeeded.")
        triggerEvent(Exit)
    }

    fun onScreenResumed() {
        if (viewState.value is MissingPermissionsError) {
            triggerEvent(CheckLocationPermissions(::onCheckLocationPermissionsResult))
        }
    }

    sealed class CardReaderConnectEvent : Event() {
        data class InitializeCardReaderManager(val onCardManagerInitialized: (manager: CardReaderManager) -> Unit) :
            CardReaderConnectEvent()

        data class CheckLocationPermissions(val onPermissionsCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

        data class CheckLocationEnabled(val onLocationEnabledCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

        data class CheckBluetoothEnabled(val onBluetoothCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

        data class RequestEnableBluetooth(val onEnableBluetoothRequestResult: (Boolean) -> Unit) :
            CardReaderConnectEvent()

        data class RequestLocationPermissions(val onPermissionsRequestResult: (Boolean) -> Unit) :
            CardReaderConnectEvent()

        object OpenPermissionsSettings : CardReaderConnectEvent()

        data class OpenLocationSettings(val onLocationSettingsClosed: () -> Unit) : CardReaderConnectEvent()
    }

    sealed class ViewState(
        val headerLabel: UiString? = null,
        @DrawableRes val illustration: Int? = null,
        @StringRes val hintLabel: Int? = null,
        val primaryActionLabel: Int? = null,
        val secondaryActionLabel: Int? = null
    ) {
        open val onPrimaryActionClicked: (() -> Unit)? = null
        open val onSecondaryActionClicked: (() -> Unit)? = null

        data class ScanningState(override val onSecondaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = UiStringRes(R.string.card_reader_connect_scanning_header),
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_scanning_hint,
            secondaryActionLabel = R.string.cancel
        )

        data class ReaderFoundState(
            override val onPrimaryActionClicked: (() -> Unit),
            override val onSecondaryActionClicked: (() -> Unit),
            val readerId: String
        ) : ViewState(
            headerLabel = UiStringRes(
                stringRes = R.string.card_reader_connect_reader_found_header,
                params = listOf(UiStringText("<b>$readerId</b>")),
                containsHtml = true
            ),
            illustration = R.drawable.img_card_reader,
            primaryActionLabel = R.string.card_reader_connect_to_reader,
            secondaryActionLabel = R.string.cancel
        )

        // TODO cardreader add multiple readers found state

        data class ConnectingState(override val onSecondaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = UiStringRes(R.string.card_reader_connect_connecting_header),
            illustration = R.drawable.img_card_reader_connecting,
            hintLabel = R.string.card_reader_connect_connecting_hint,
            secondaryActionLabel = R.string.cancel
        )

        // TODO cardreader add error state
        data class MissingPermissionsError(
            override val onPrimaryActionClicked: () -> Unit,
            override val onSecondaryActionClicked: () -> Unit
        ) : ViewState(
            headerLabel = UiStringRes(R.string.card_reader_connect_failed_header),
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_missing_permissions_hint,
            primaryActionLabel = R.string.card_reader_connect_open_permission_settings,
            secondaryActionLabel = R.string.cancel
        )

        data class LocationDisabledError(
            override val onPrimaryActionClicked: () -> Unit,
            override val onSecondaryActionClicked: () -> Unit
        ) : ViewState(
            headerLabel = UiStringRes(R.string.card_reader_connect_failed_header),
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_location_provider_disabled_hint,
            primaryActionLabel = R.string.card_reader_connect_open_location_settings,
            secondaryActionLabel = R.string.cancel
        )

        data class BluetoothDisabledError(
            override val onPrimaryActionClicked: () -> Unit,
            override val onSecondaryActionClicked: () -> Unit
        ) : ViewState(
            headerLabel = UiStringRes(R.string.card_reader_connect_failed_header),
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_bluetooth_disabled_hint,
            primaryActionLabel = R.string.card_reader_connect_open_permission_settings,
            secondaryActionLabel = R.string.cancel
        )
    }
}
