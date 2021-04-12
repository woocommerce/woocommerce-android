package com.woocommerce.android.cardreader

import com.stripe.stripeterminal.model.external.Reader

class CardReaderImpl(val cardReader: Reader) : CardReader {
    override fun getId(): String? = cardReader.serialNumber
}
