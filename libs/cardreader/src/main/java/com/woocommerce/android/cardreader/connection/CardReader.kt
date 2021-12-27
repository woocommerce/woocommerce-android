package com.woocommerce.android.cardreader.connection

interface CardReader {
    val id: String?
    val type: String
    val currentBatteryLevel: Float?
    val firmwareVersion: String
    val locationId: String?
}
