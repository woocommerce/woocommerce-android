package com.woocommerce.android.cardreader.internal.connection

import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType

sealed class BluetoothCardReaderMessages {
    data class CardReaderDisplayMessage(val message: AdditionalInfoType) : BluetoothCardReaderMessages()
    data class CardReaderInputMessage(val options: String) : BluetoothCardReaderMessages()
}
