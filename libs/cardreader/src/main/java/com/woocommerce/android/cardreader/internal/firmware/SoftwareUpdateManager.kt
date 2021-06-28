package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.Initializing
import com.woocommerce.android.cardreader.SoftwareUpdateStatus
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
    suspend fun updateSoftware() = flow {
        emit(SoftwareUpdateStatus.Initializing)

        when (val updateStatus = checkUpdatesAction.checkUpdates()) {
            CheckSoftwareUpdates.UpToDate -> emit(SoftwareUpdateStatus.UpToDate)
            is CheckSoftwareUpdates.Failed -> emit(SoftwareUpdateStatus.Failed(updateStatus.e.errorMessage))
            is CheckSoftwareUpdates.UpdateAvailable -> installUpdate(updateStatus.updateData)
        }
    }

    suspend fun softwareUpdateStatus() = flow {
        emit(Initializing)

        when (checkUpdatesAction.checkUpdates()) {
            CheckSoftwareUpdates.UpToDate -> emit(SoftwareUpdateAvailability.UpToDate)
            is CheckSoftwareUpdates.Failed -> emit(SoftwareUpdateAvailability.CheckForUpdatesFailed)
            is CheckSoftwareUpdates.UpdateAvailable -> emit(SoftwareUpdateAvailability.UpdateAvailable)
        }
    }

    private suspend fun FlowCollector<SoftwareUpdateStatus>.installUpdate(updateData: ReaderSoftwareUpdate) {
        installSoftwareUpdateAction.installUpdate(updateData).collect { status ->
            when (status) {
                is InstallSoftwareUpdateStatus.Failed -> emit(SoftwareUpdateStatus.Failed(status.e.errorMessage))
                is InstallSoftwareUpdateStatus.Installing -> emit(SoftwareUpdateStatus.Installing(status.progress))
                InstallSoftwareUpdateStatus.Success -> emit(SoftwareUpdateStatus.Success)
            }
        }
    }
}
