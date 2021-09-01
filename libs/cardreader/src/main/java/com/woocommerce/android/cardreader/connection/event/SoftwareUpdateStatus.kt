package com.woocommerce.android.cardreader.connection.event

sealed interface SoftwareUpdateInProgress

sealed class SoftwareUpdateStatus {
    object InstallationStarted : SoftwareUpdateStatus(), SoftwareUpdateInProgress
    data class Installing(val progress: Float) : SoftwareUpdateStatus(), SoftwareUpdateInProgress
    object Success : SoftwareUpdateStatus()
    data class Failed(val message: String?) : SoftwareUpdateStatus()
    object NotAvailable : SoftwareUpdateStatus()
}
