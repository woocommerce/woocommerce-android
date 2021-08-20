package com.woocommerce.android.cardreader.internal.firmware.actions

import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
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
    }
}
