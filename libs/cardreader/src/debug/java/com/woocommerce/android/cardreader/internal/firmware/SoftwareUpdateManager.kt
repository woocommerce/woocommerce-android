package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.woocommerce.android.cardreader.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Initializing
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

internal class SoftwareUpdateManager(
    private val checkUpdatesAction: CheckSoftwareUpdatesAction,
    private val installSoftwareUpdateAction: InstallSoftwareUpdateAction
) {
    suspend fun updateSoftware() = flow<SoftwareUpdateStatus> {
        emit(Initializing)
        val updateStatus = checkUpdatesAction.checkUpdates()

        when (updateStatus) {
            CheckSoftwareUpdates.UpToDate -> emit(SoftwareUpdateStatus.UpToDate)
            is CheckSoftwareUpdates.Failed -> emit(SoftwareUpdateStatus.CheckForUpdatesFailed)
            is CheckSoftwareUpdates.UpdateAvailable -> installUpdate(updateStatus.updateData)
        }
    }

    private suspend fun FlowCollector<SoftwareUpdateStatus>.installUpdate(updateData: ReaderSoftwareUpdate) {
        installSoftwareUpdateAction.installUpdate(updateData).collect {
            when (it) {
                is InstallSoftwareUpdateStatus.Failed -> emit(SoftwareUpdateStatus.Failed(it.e.errorMessage))
                is InstallSoftwareUpdateStatus.Installing -> emit(SoftwareUpdateStatus.Installing(it.progress))
                InstallSoftwareUpdateStatus.Success -> emit(SoftwareUpdateStatus.Success)
            }
        }
    }
}
