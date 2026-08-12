package com.woocommerce.android.cardreader.remote

class CardReaderRemoteConnectionLostException(cause: Throwable?) :
    IllegalStateException("Connection to phone reader was lost", cause)
