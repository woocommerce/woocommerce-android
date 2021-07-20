package com.woocommerce.android.cardreader

import com.stripe.stripeterminal.external.models.Reader

class CardReaderImpl(val cardReader: Reader) : CardReader {
    override val id: String?
        get() = cardReader.serialNumber
    override val type: String
        get() = cardReader.deviceType.name
    override val currentBatteryLevel: Float?
        get() = cardReader.batteryLevel
}
