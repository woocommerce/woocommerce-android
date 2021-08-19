package com.woocommerce.android.cardreader.internal.firmware.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

internal class InstallAvailableSoftwareUpdateAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper
) {
    sealed class InstallSoftwareUpdateStatus {
        data class Installing(val progress: Float) : InstallSoftwareUpdateStatus()
        object Success : InstallSoftwareUpdateStatus()
        data class Failed(val e: TerminalException) : InstallSoftwareUpdateStatus()
    }

    fun installUpdate() {}
}

private val noopCallback = object : Callback {
    override fun onFailure(e: TerminalException) {}

    override fun onSuccess() {}
}
