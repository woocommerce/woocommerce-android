package com.woocommerce.android.cardreader.internal.firmware

import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

internal class SoftwareUpdateManager(
    private val checkUpdatesAction: CheckSoftwareUpdatesAction,
    private val installAvailableSoftwareUpdateAction: InstallAvailableSoftwareUpdateAction
) {
    suspend fun updateSoftware() = flow {
        when (val updateStatus = checkUpdatesAction.checkUpdates()) {
            is CheckSoftwareUpdates.Failed -> emit(SoftwareUpdateStatus.Failed(updateStatus.e.errorMessage))
            is CheckSoftwareUpdates.UpdateAvailable -> installUpdate()
        }
    }

    private suspend fun FlowCollector<SoftwareUpdateStatus>.installUpdate() {
        installAvailableSoftwareUpdateAction.installUpdate().collect { status ->
            when (status) {
                is InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Failed -> emit(
                    SoftwareUpdateStatus.Failed(
                        status.e.errorMessage
                    )
                )
                is InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Installing -> emit(
                    SoftwareUpdateStatus.Installing(status.progress)
                )
                InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Success -> emit(
                    SoftwareUpdateStatus.Success
                )
            }
        }
    }
}
