package com.woocommerce.android.cardreader.internal.firmware.actions

import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.ReaderSoftwareUpdateListener
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

internal class InstallSoftwareUpdateAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper
) {
    sealed class InstallSoftwareUpdateStatus {
        data class Installing(val progress: Float) : InstallSoftwareUpdateStatus()
        object Success : InstallSoftwareUpdateStatus()
        data class Failed(val e: TerminalException) : InstallSoftwareUpdateStatus()
    }

    fun installUpdate(update: ReaderSoftwareUpdate) = callbackFlow<InstallSoftwareUpdateStatus> {
        var cancelable: Cancelable? = null
        try {
            cancelable = terminal.installSoftwareUpdate(update, object : ReaderSoftwareUpdateListener {
                override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
                    this@callbackFlow.sendBlocking(Installing(progress))
                }
            }, object : Callback {
                override fun onSuccess() {
                    this@callbackFlow.sendBlocking(Success)
                    this@callbackFlow.close()
                }

                override fun onFailure(e: TerminalException) {
                    this@callbackFlow.sendBlocking(Failed(e))
                    this@callbackFlow.close()
                }
            })
        } finally {
            awaitClose {
                cancelable?.takeIf { !it.isCompleted }?.cancel(noopCallback)
            }
        }
    }
}

private val noopCallback = object : Callback {
    override fun onFailure(e: TerminalException) {}

    override fun onSuccess() {}
}
