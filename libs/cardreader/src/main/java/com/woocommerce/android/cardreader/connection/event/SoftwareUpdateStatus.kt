package com.woocommerce.android.cardreader.connection.event

sealed class SoftwareUpdateStatus : CardReaderEvent {
    object InstallationStarted : SoftwareUpdateStatus()
    data class Installing(val progress: Float) : SoftwareUpdateStatus()
    object Success : SoftwareUpdateStatus()
    data class Failed(val message: String?) : SoftwareUpdateStatus()
    object UpToDate : SoftwareUpdateStatus()
}
