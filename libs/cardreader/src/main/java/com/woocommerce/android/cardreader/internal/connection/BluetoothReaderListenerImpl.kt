package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.BluetoothReaderListener
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.payments.AdditionalInfoMapper
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class BluetoothReaderListenerImpl(
    private val logWrapper: LogWrapper,
    private val additionalInfoMapper: AdditionalInfoMapper,
) : BluetoothReaderListener {
    private val _updateStatusEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
    val updateStatusEvents = _updateStatusEvents.asStateFlow()

    private val _updateAvailabilityEvents =
        MutableStateFlow<SoftwareUpdateAvailability>(SoftwareUpdateAvailability.NotAvailable)
    val updateAvailabilityEvents = _updateAvailabilityEvents.asStateFlow()

    private var bluetoothCardReaderMessagesObserver: BluetoothCardReaderMessagesObserver? = null

    fun registerBluetoothCardReaderMessagesObserver(messagesObserver: BluetoothCardReaderMessagesObserver) {
        bluetoothCardReaderMessagesObserver = messagesObserver
    }

    fun unregisterBluetoothCardReaderMessages() {
        bluetoothCardReaderMessagesObserver = null
    }

    override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
        logWrapper.d(LOG_TAG, "onFinishInstallingUpdate: $update $e")
        if (e == null) {
            _updateAvailabilityEvents.value = SoftwareUpdateAvailability.NotAvailable
            _updateStatusEvents.value = SoftwareUpdateStatus.Success
        } else {
            _updateStatusEvents.value = SoftwareUpdateStatus.Failed(e.message)
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
        logWrapper.d(LOG_TAG, "onStartInstallingUpdate: $update $cancelable")
        _updateStatusEvents.value = SoftwareUpdateStatus.InstallationStarted
    }

    override fun onReportLowBatteryWarning() {
        logWrapper.d(LOG_TAG, "onReportLowBatteryWarning")
    }

    override fun onReportReaderEvent(event: ReaderEvent) {
        logWrapper.d(LOG_TAG, "onReportReaderEvent: $event")
    }

    override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
        logWrapper.d(LOG_TAG, "onRequestReaderDisplayMessage: $message")
        bluetoothCardReaderMessagesObserver?.let { bluetoothCardReaderMessagesObserver ->
            bluetoothCardReaderMessagesObserver
                .sendMessage(BluetoothCardReaderMessages.CardReaderDisplayMessage(additionalInfoMapper.map(message)))
        }
    }

    override fun onRequestReaderInput(options: ReaderInputOptions) {
        logWrapper.d(LOG_TAG, "onRequestReaderInput: $options")
        bluetoothCardReaderMessagesObserver?.let { bluetoothCardReaderMessagesObserver ->
            bluetoothCardReaderMessagesObserver
                .sendMessage(BluetoothCardReaderMessages.CardReaderInputMessage(options.toString()))
        }
    }
}
