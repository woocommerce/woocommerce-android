package com.woocommerce.android.cardreader.connection.event

sealed interface SoftwareUpdateInProgress

sealed class SoftwareUpdateStatus : CardReaderEvent {
    object InstallationStarted : SoftwareUpdateStatus(), SoftwareUpdateInProgress
    data class Installing(val progress: Float) : SoftwareUpdateStatus(), SoftwareUpdateInProgress
    object Success : SoftwareUpdateStatus()
    data class Failed(val message: String?) : SoftwareUpdateStatus()
    object UpToDate : SoftwareUpdateStatus()
    object NotAvailable : SoftwareUpdateStatus()
}
