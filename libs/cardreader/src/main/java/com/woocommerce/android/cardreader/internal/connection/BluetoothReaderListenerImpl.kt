package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.BluetoothReaderListener
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.CardReaderEvent
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class BluetoothReaderListenerImpl(
    private val logWrapper: LogWrapper,
) : BluetoothReaderListener {
    private val _events = MutableStateFlow<CardReaderEvent>(CardReaderEvent.Initialisation)
    val events: StateFlow<CardReaderEvent> = _events

    override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
        logWrapper.d(LOG_TAG, "onFinishInstallingUpdate: $update $e")
        _events.value = if (e == null) {
            SoftwareUpdateStatus.Success
        } else {
            SoftwareUpdateStatus.Failed(e.message)
        }
    }

    override fun onReportAvailableUpdate(update: ReaderSoftwareUpdate) {
        logWrapper.d(LOG_TAG, "onReportAvailableUpdate: $update")
        _events.value = SoftwareUpdateAvailability.Available
    }

    override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
        logWrapper.d(LOG_TAG, "onReportReaderSoftwareUpdateProgress: $progress")
        _events.value = SoftwareUpdateStatus.Installing(progress)
    }

    override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: Cancelable?) {
        logWrapper.d(LOG_TAG, "onStartInstallingUpdate: $update $cancelable")
        _events.value = SoftwareUpdateStatus.InstallationStarted
    }

    override fun onReportLowBatteryWarning() {
        logWrapper.d(LOG_TAG, "onReportLowBatteryWarning")
    }

    override fun onReportReaderEvent(event: ReaderEvent) {
        logWrapper.d(LOG_TAG, "onReportReaderEvent: $event")
    }

    override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
        logWrapper.d(LOG_TAG, "onRequestReaderDisplayMessage: $message")
    }

    override fun onRequestReaderInput(options: ReaderInputOptions) {
        logWrapper.d(LOG_TAG, "onRequestReaderInput: $options")
    }
}
