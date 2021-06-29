package com.woocommerce.android.cardreader

interface CardReader {
    val id: String?
    val type: String?
    val currentBatteryLevel: Float?
}
