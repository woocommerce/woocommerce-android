package com.woocommerce.android.cardreader.connection.event

sealed class SoftwareUpdateAvailability {
    object Available : SoftwareUpdateAvailability()
    object NotAvailable : SoftwareUpdateAvailability()
}
