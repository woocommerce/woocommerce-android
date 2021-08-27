package com.woocommerce.android.cardreader.connection.event

sealed class SoftwareUpdateAvailability : CardReaderEvent {
    object Available : SoftwareUpdateAvailability()
    object NotAvailable : SoftwareUpdateAvailability()
}
