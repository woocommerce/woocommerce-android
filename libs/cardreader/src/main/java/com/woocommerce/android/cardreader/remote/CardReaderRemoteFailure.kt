package com.woocommerce.android.cardreader.remote

internal class CardReaderRemoteFailure(
    val error: CardReaderRemoteError,
    override val message: String,
) : Exception(message)
