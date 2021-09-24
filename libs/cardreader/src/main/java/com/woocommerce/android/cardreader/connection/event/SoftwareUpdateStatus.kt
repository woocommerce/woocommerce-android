package com.woocommerce.android.cardreader.connection.event

sealed interface SoftwareUpdateInProgress

sealed class SoftwareUpdateStatus {
    object InstallationStarted : SoftwareUpdateStatus(), SoftwareUpdateInProgress
    data class Installing(val progress: Float) : SoftwareUpdateStatus(), SoftwareUpdateInProgress
    object Success : SoftwareUpdateStatus()
    data class Failed(val errorType: SoftwareUpdateStatusErrorType, val message: String?) : SoftwareUpdateStatus()
    object Unknown : SoftwareUpdateStatus()
}

sealed class SoftwareUpdateStatusErrorType {
    data class BatteryLow(val currentBatteryLevel: Float?) : SoftwareUpdateStatusErrorType()
    object Interrupted : SoftwareUpdateStatusErrorType()
    object ReaderError : SoftwareUpdateStatusErrorType()
    object ServerError : SoftwareUpdateStatusErrorType()
    object Failed : SoftwareUpdateStatusErrorType()
}
