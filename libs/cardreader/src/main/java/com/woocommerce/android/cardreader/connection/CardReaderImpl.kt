package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.model.external.Reader

class CardReaderImpl(val cardReader: Reader) : CardReader {
    override val id: String?
        get() = cardReader.serialNumber
    override val type: String
        get() = cardReader.deviceType.name
    override val currentBatteryLevel: Float?
        get() = cardReader.batteryLevel
    override val firmwareVersion: String
        get() = cardReader.softwareVersion
}
