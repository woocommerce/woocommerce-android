package com.woocommerce.android.cardreader.internal.connection

abstract class BluetoothCardReaderMessagesObserver : BluetoothCardReaderObserver {

    abstract fun sendMessage(message: BluetoothCardReaderMessages)
}
