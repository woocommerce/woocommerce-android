package com.woocommerce.android.cardreader

import com.stripe.stripeterminal.model.external.Reader

class CardReaderImpl(val cardReader: Reader) : CardReader {
    override val id: String?
        get() = cardReader.serialNumber
    override val currentBatteryLevel: Float?
        get() = cardReader.batteryLevel
}
