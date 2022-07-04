package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.BluetoothReaderListener
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.models.BatteryStatus
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages.CardReaderNoMessage
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus.StatusChanged
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus.Unknown
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus.Warning
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.connection.event.toLocalBatteryStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.payments.AdditionalInfoMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class BluetoothReaderListenerImpl(
    private val logWrapper: LogWrapper,
    private val additionalInfoMapper: AdditionalInfoMapper,
    private val updateErrorMapper: UpdateErrorMapper,
) : BluetoothReaderListener {
    private val _updateStatusEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
    val updateStatusEvents = _updateStatusEvents.asStateFlow()

    private val _updateAvailabilityEvents =
        MutableStateFlow<SoftwareUpdateAvailability>(SoftwareUpdateAvailability.NotAvailable)
    val updateAvailabilityEvents = _updateAvailabilityEvents.asStateFlow()

    private val _displayMessagesEvents = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
    val displayMessagesEvents = _displayMessagesEvents.asStateFlow()

    private val _batteryStatusEvents = MutableStateFlow<CardReaderBatteryStatus>(Unknown)
    val batteryStatusEvents = _batteryStatusEvents.asStateFlow()

    var cancelUpdateAction: Cancelable? = null

    override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
        logWrapper.d(LOG_TAG, "onFinishInstallingUpdate: $update $e")
        if (e == null) {
            _updateAvailabilityEvents.value = SoftwareUpdateAvailability.NotAvailable
            _updateStatusEvents.value = SoftwareUpdateStatus.Success
        } else {
            _updateStatusEvents.value = SoftwareUpdateStatus.Failed(
                updateErrorMapper.map(e.errorCode),
                e.message
            )
        }
    }

    override fun onReportAvailableUpdate(update: ReaderSoftwareUpdate) {
        logWrapper.d(LOG_TAG, "onReportAvailableUpdate: $update")
        _updateAvailabilityEvents.value = SoftwareUpdateAvailability.Available
    }

    override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
        logWrapper.d(LOG_TAG, "onReportReaderSoftwareUpdateProgress: $progress")
        _updateStatusEvents.value = SoftwareUpdateStatus.Installing(progress)
    }

    override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: Cancelable?) {
        cancelUpdateAction = cancelable
        logWrapper.d(LOG_TAG, "onStartInstallingUpdate: $update $cancelable")
        _updateStatusEvents.value = SoftwareUpdateStatus.InstallationStarted
    }

    override fun onReportLowBatteryWarning() {
        logWrapper.d(LOG_TAG, "onReportLowBatteryWarning")
        _batteryStatusEvents.value = Warning
    }

    override fun onBatteryLevelUpdate(batteryLevel: Float, batteryStatus: BatteryStatus, isCharging: Boolean) {
        logWrapper.d(
            LOG_TAG,
            "onBatteryLevelUpdate: batteryStatus: $batteryStatus, batteryLevel: $batteryLevel, isCharging: $isCharging"
        )
        _batteryStatusEvents.value = StatusChanged(
            batteryLevel,
            batteryStatus.toLocalBatteryStatus(),
            isCharging,
        )
    }

    override fun onReportReaderEvent(event: ReaderEvent) {
        logWrapper.d(LOG_TAG, "onReportReaderEvent: $event")
    }

    override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
        logWrapper.d(LOG_TAG, "onRequestReaderDisplayMessage: $message")
        _displayMessagesEvents.value = BluetoothCardReaderMessages
            .CardReaderDisplayMessage(additionalInfoMapper.map(message))
    }

    override fun onRequestReaderInput(options: ReaderInputOptions) {
        logWrapper.d(LOG_TAG, "onRequestReaderInput: $options")
        _displayMessagesEvents.value = BluetoothCardReaderMessages.CardReaderInputMessage(options.toString())
    }

    fun resetConnectionState() {
        _updateStatusEvents.value = SoftwareUpdateStatus.Unknown
        _updateAvailabilityEvents.value = SoftwareUpdateAvailability.NotAvailable
    }

    fun resetDisplayMessage() {
        _displayMessagesEvents.value = CardReaderNoMessage
    }
}
