package com.woocommerce.android.cardreader

sealed class SoftwareUpdateStatus {
    object Initializing : SoftwareUpdateStatus()
    data class Installing(val progress: Float) : SoftwareUpdateStatus()
    object Success : SoftwareUpdateStatus()
    data class Failed(val message: String?) : SoftwareUpdateStatus()
    object UpToDate : SoftwareUpdateStatus()
}
