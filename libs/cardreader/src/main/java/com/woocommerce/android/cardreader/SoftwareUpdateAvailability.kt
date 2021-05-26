package com.woocommerce.android.cardreader

sealed class SoftwareUpdateAvailability {
    object Initializing : SoftwareUpdateAvailability()
    object UpToDate : SoftwareUpdateAvailability()
    object UpdateAvailable : SoftwareUpdateAvailability()
    object CheckForUpdatesFailed : SoftwareUpdateAvailability()
}
