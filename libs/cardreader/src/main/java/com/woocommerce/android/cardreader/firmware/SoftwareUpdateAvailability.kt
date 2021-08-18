package com.woocommerce.android.cardreader.firmware

sealed class SoftwareUpdateAvailability {
    object Initializing : SoftwareUpdateAvailability()
    object UpToDate : SoftwareUpdateAvailability()
    object UpdateAvailable : SoftwareUpdateAvailability()
    object CheckForUpdatesFailed : SoftwareUpdateAvailability()
}
