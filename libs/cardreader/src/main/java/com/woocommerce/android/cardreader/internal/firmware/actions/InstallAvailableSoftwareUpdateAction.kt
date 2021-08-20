package com.woocommerce.android.cardreader.internal.firmware.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.callbackFlow

internal class InstallAvailableSoftwareUpdateAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper
) {
    sealed class InstallSoftwareUpdateStatus {
        data class Installing(val progress: Float) : InstallSoftwareUpdateStatus()
        object Success : InstallSoftwareUpdateStatus()
        data class Failed(val e: TerminalException) : InstallSoftwareUpdateStatus()
    }

    @Suppress("EmptyFunctionBlock")
    fun installUpdate() = callbackFlow<InstallSoftwareUpdateStatus> {}
}

@Suppress("EmptyFunctionBlock")
private val noopCallback = object : Callback {
    override fun onFailure(e: TerminalException) {}

    override fun onSuccess() {}
}
