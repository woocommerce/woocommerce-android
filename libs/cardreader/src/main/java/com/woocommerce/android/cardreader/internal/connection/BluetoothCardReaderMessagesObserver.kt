package com.woocommerce.android.cardreader.internal.connection

interface BluetoothCardReaderMessagesObserver {
    fun sendMessage(message: BluetoothCardReaderMessages)
}
