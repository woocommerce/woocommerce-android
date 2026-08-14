package com.woocommerce.android.cardreader.internal.connection

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.InternetReaderListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration.BluetoothConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionConfiguration.InternetConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionConfiguration.TapToPayConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TapUseCase
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders.BuiltInReaders
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders.ExternalReaders
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.UnspecifiedReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

private const val ARTIFICIAL_STATUS_UPDATE_DELAY_IN_MILLIS = 500L
private const val LOG_TAG = "ConnectionManager"

@Suppress("LongParameterList")
internal class ConnectionManager(
    private val terminal: TerminalWrapper,
    private val bluetoothReaderListener: BluetoothReaderListenerImpl,
    private val tapToPayReaderListener: TapToPayReaderListenerImpl,
    private val discoverReadersAction: DiscoverReadersAction,
    private val terminalListenerImpl: TerminalListenerImpl,
    private val application: Application,
    private val logWrapper: LogWrapper,
) {
    val softwareUpdateStatus = bluetoothReaderListener.updateStatusEvents
    val softwareUpdateAvailability = bluetoothReaderListener.updateAvailabilityEvents
    val batteryStatus = bluetoothReaderListener.batteryStatusEvents
    val displayBluetoothCardReaderMessages = bluetoothReaderListener.displayMessagesEvents
    fun discoverReaders(isSimulated: Boolean, cardReaderTypesToDiscover: CardReaderTypesToDiscover) =
        when (cardReaderTypesToDiscover) {
            is SpecificReaders -> {
                when (cardReaderTypesToDiscover) {
                    is BuiltInReaders -> {
                        if (application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            error("ACCESS_FINE_LOCATION permission is required to discover built-in readers")
                        }
                        discoverReadersAction.discoverBuildInReaders(isSimulated)
                    }

                    is ExternalReaders -> {
                        checkIfNecessaryPermissionsAreGranted()
                        // Stripe SDK allows only one discoverReaders operation at a time. Run
                        // internet discovery first (short-running). Skip the long-running
                        // bluetooth scan if internet already returned a reader, so a BT timeout
                        // doesn't overwrite a successful result in the UI.
                        if (cardReaderTypesToDiscover.externalReaders.any { it.isInternetReader }) {
                            flow {
                                var foundInternetReader = false
                                discoverReadersAction.discoverInternetReaders(isSimulated = isSimulated)
                                    .collect { event ->
                                        if (event is DiscoverReadersStatus.FoundReaders &&
                                            event.readers.isNotEmpty()
                                        ) {
                                            foundInternetReader = true
                                        }
                                        emit(event)
                                    }
                                if (!foundInternetReader) {
                                    emitAll(discoverReadersAction.discoverExternalReaders(isSimulated))
                                }
                            }
                        } else {
                            discoverReadersAction.discoverExternalReaders(isSimulated)
                        }
                    }
                }
            }

            UnspecifiedReaders -> {
                checkIfNecessaryPermissionsAreGranted()
                // Stripe SDK serializes discoverReaders calls. Internet and Tap to Pay are
                // short-running, so chain them before the long-running bluetooth discovery.
                flow {
                    emitAll(discoverReadersAction.discoverInternetReaders(isSimulated = isSimulated))
                    emitAll(discoverReadersAction.discoverBuildInReaders(isSimulated))
                    emitAll(discoverReadersAction.discoverExternalReaders(isSimulated))
                }
            }
        }.map { state ->
            when (state) {
                is DiscoverReadersStatus.Started -> {
                    CardReaderDiscoveryEvents.Started
                }

                is DiscoverReadersStatus.Failure -> {
                    when (state.exception.errorCode) {
                        TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
                        TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
                        TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_ANDROID_VERSION,
                        TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_PROCESSOR,
                        TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT ->
                            CardReaderDiscoveryEvents.FailedTapToPayDeviceUnsupported(state.exception.errorMessage)
                        else -> CardReaderDiscoveryEvents.Failed(state.exception.errorMessage)
                    }
                }

                is DiscoverReadersStatus.FoundReaders -> {
                    val filtering: (Reader) -> Boolean = when (cardReaderTypesToDiscover) {
                        is SpecificReaders -> { reader ->
                            cardReaderTypesToDiscover.readers.map { it.name }.contains(reader.deviceType.name)
                        }

                        UnspecifiedReaders -> { _ -> true }
                    }
                    CardReaderDiscoveryEvents.ReadersFound(state.readers.filter(filtering).map { CardReaderImpl(it) })
                }

                DiscoverReadersStatus.Success -> {
                    CardReaderDiscoveryEvents.Succeeded
                }
            }
        }

    private fun checkIfNecessaryPermissionsAreGranted() {
        val isAtLeastAndroidS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val permissionsToCheck = mutableMapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to "ACCESS_FINE_LOCATION permission is " +
                "required to discover external readers"
        )

        if (isAtLeastAndroidS) {
            permissionsToCheck[Manifest.permission.BLUETOOTH_CONNECT] = "BLUETOOTH_CONNECT permission is " +
                "required to discover external readers"
            permissionsToCheck[Manifest.permission.BLUETOOTH_SCAN] = "BLUETOOTH_SCAN permission is " +
                "required to discover external readers"
        }

        permissionsToCheck.forEach { (permission, errorMessage) ->
            if (application.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                error(errorMessage)
            }
        }
    }

    suspend fun startConnectionToReader(cardReader: CardReader, locationId: String) {
        (cardReader as CardReaderImpl).let {
            updateReaderStatus(CardReaderStatus.Connecting)
            try {
                val reader = when (it.cardReader.deviceType) {
                    DeviceType.TAP_TO_PAY_DEVICE -> connectToBuiltInReader(cardReader, locationId)
                    DeviceType.WISEPOS_E,
                    DeviceType.STRIPE_S700,
                    DeviceType.STRIPE_S710,
                    DeviceType.STRIPE_T600,
                    DeviceType.STRIPE_T610 -> connectToInternetReader(cardReader)
                    else -> connectToExternalReader(cardReader, locationId)
                }
                updateReaderStatus(CardReaderStatus.Connected(CardReaderImpl(reader)))
            } catch (e: TerminalException) {
                updateReaderStatus(
                    CardReaderStatus.NotConnected(
                        errorCode = e.errorCode.toErrorCode(),
                        errorMessage = e.errorMessage,
                    )
                )
            }
        }
    }

    suspend fun disconnectReader(): Boolean {
        return try {
            terminal.disconnectReader()
            updateReaderStatus(CardReaderStatus.NotConnected())
            true
        } catch (e: TerminalException) {
            logWrapper.e(LOG_TAG, "Failed to disconnect reader: ${e.errorMessage}")
            updateReaderStatus(CardReaderStatus.NotConnected())
            false
        }
    }

    fun cancelReconnection() {
        val callback = object : Callback {
            override fun onFailure(e: TerminalException) {
                updateReaderStatus(CardReaderStatus.NotConnected())
            }

            override fun onSuccess() {
                updateReaderStatus(CardReaderStatus.NotConnected())
            }
        }
        bluetoothReaderListener.cancelReconnection(callback)
        tapToPayReaderListener.cancelReconnection(callback)
    }

    private fun startStateResettingJobIfNeeded(currentStatus: CardReaderStatus) {
        if (currentStatus !is CardReaderStatus.Connecting) return

        val connectedScope = CoroutineScope(Dispatchers.Main)
        connectedScope.launch {
            terminalListenerImpl.readerStatus.collect { connectionStatus ->
                if (connectionStatus is CardReaderStatus.NotConnected) {
                    /**
                     * This delay is a workaround to avoid situations in which the state is reset to Unknown before
                     * the consumer gets a chance to handle it. For example, when the consumer initiates the connection
                     * flow and there is a required update, the client might want to navigate to the sw update flow.
                     * However, the Stripe SDK starts the update flow immediately and if it fails on LOW_BATTERY_ERROR
                     * the state is reset to Unknown (no ongoing update) before the client even got a chance to
                     * navigate to the sw update flow. More info on PR 12912.
                     */
                    delay(ARTIFICIAL_STATUS_UPDATE_DELAY_IN_MILLIS)
                    bluetoothReaderListener.resetConnectionState()
                    connectedScope.cancel()
                }
            }
        }
    }

    fun resetBluetoothCardReaderDisplayMessage() {
        bluetoothReaderListener.resetDisplayMessage()
    }

    private fun updateReaderStatus(status: CardReaderStatus) {
        terminalListenerImpl.updateReaderStatus(status)
        startStateResettingJobIfNeeded(status)
    }

    fun setupTapToPayUx(config: CardReaderManager.TapToPayUxConfig) {
        terminal.setupTapToPayUx(config)
    }

    private suspend fun connectToExternalReader(
        cardReader: CardReaderImpl,
        locationId: String
    ): Reader {
        return terminal.connectToReader(
            cardReader.cardReader,
            BluetoothConnectionConfiguration(locationId, true, bluetoothReaderListener)
        )
    }

    private suspend fun connectToBuiltInReader(
        cardReader: CardReaderImpl,
        locationId: String
    ): Reader {
        return terminal.connectToMobile(
            cardReader.cardReader,
            TapToPayConnectionConfiguration(
                TapUseCase.Pay(locationId),
                autoReconnectOnUnexpectedDisconnect = true,
                tapToPayReaderListener
            )
        )
    }

    private suspend fun connectToInternetReader(cardReader: CardReaderImpl): Reader {
        return terminal.connectToInternet(
            cardReader.cardReader,
            InternetConnectionConfiguration(
                internetReaderListener = object : InternetReaderListener {},
                failIfInUse = false
            )
        )
    }

    private fun TerminalErrorCode.toErrorCode(): CardReaderStatus.NotConnected.ErrorCode =
        when (this) {
            TerminalErrorCode.READER_BATTERY_CRITICALLY_LOW ->
                CardReaderStatus.NotConnected.ErrorCode.BATTERY_CRITICALLY_LOW

            else -> CardReaderStatus.NotConnected.ErrorCode.OTHER
        }
}
