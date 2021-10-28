package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.models.Reader

internal class CardReaderImpl(val cardReader: Reader) : CardReader {
    override val id: String?
        get() = cardReader.serialNumber
    override val type: String
        get() = cardReader.deviceType.name
    override val currentBatteryLevel: Float?
        get() = cardReader.batteryLevel
    override val firmwareVersion: String
        get() = cardReader.softwareVersion
    override val locationId: String?
        get() = cardReader.location?.id
}
