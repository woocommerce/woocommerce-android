package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate.UpdateTimeEstimate
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate.UpdateTimeEstimate.FIVE_TO_FIFTEEN_MINUTES
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate.UpdateTimeEstimate.LESS_THAN_ONE_MINUTE
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate.UpdateTimeEstimate.ONE_TO_TWO_MINUTES
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate.UpdateTimeEstimate.TWO_TO_FIVE_MINUTES
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.Initializing
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.UpdateAvailable.TimeEstimate
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

        when (val status = checkUpdatesAction.checkUpdates()) {
            CheckSoftwareUpdates.UpToDate -> emit(SoftwareUpdateAvailability.UpToDate)
            is CheckSoftwareUpdates.Failed -> emit(SoftwareUpdateAvailability.CheckForUpdatesFailed)
            is CheckSoftwareUpdates.UpdateAvailable ->
                emit(
                    SoftwareUpdateAvailability.UpdateAvailable(
                        hasConfigUpdate = status.updateData.hasConfigUpdate,
                        hasFirmwareUpdate = status.updateData.hasFirmwareUpdate,
                        hasKeyUpdate = status.updateData.hasKeyUpdate,
                        timeEstimate = status.updateData.timeEstimate.mapToTimeEstimate(),
                        version = status.updateData.version
                    )
                )
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

    private fun UpdateTimeEstimate.mapToTimeEstimate() =
        when (this) {
            LESS_THAN_ONE_MINUTE -> TimeEstimate.LESS_THAN_ONE_MINUTE
            ONE_TO_TWO_MINUTES -> TimeEstimate.ONE_TO_TWO_MINUTES
            TWO_TO_FIVE_MINUTES -> TimeEstimate.TWO_TO_FIVE_MINUTES
            FIVE_TO_FIFTEEN_MINUTES -> TimeEstimate.FIVE_TO_FIFTEEN_MINUTES
        }
}
