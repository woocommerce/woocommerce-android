package com.woocommerce.android.cardreader.internal.firmware.actions

import com.stripe.stripeterminal.callable.ReaderSoftwareUpdateCallback
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates.UpToDate
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates.UpdateAvailable
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class CheckSoftwareUpdatesAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper
) {
    sealed class CheckSoftwareUpdates {
        object UpToDate : CheckSoftwareUpdates()
        data class UpdateAvailable(val updateData: ReaderSoftwareUpdate) : CheckSoftwareUpdates()
        data class Failed(val e: TerminalException) : CheckSoftwareUpdates()
    }

    suspend fun checkUpdates() = suspendCoroutine<CheckSoftwareUpdates> { continuation ->
        terminal.checkForUpdate(object : ReaderSoftwareUpdateCallback {
            override fun onSuccess(updateData: ReaderSoftwareUpdate?) {
                if (isUpdateAvailable(updateData)) {
                    continuation.resume(UpdateAvailable(requireNotNull(updateData)))
                } else {
                    continuation.resume(UpToDate)
                }
            }

            override fun onFailure(e: TerminalException) {
                // todo cardreader log the exception stack trace
                logWrapper.e("CardReader", "Checking for updates failed: ${e.errorMessage}")
                continuation.resume(Failed(e))
            }
        })
    }

    private fun isUpdateAvailable(updateData: ReaderSoftwareUpdate?): Boolean =
        updateData
            ?.let { updateData.hasConfigUpdate || updateData.hasFirmwareUpdate || updateData.hasKeyUpdate }
            ?: false
}
