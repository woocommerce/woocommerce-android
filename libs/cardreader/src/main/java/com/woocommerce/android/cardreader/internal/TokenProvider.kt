package com.woocommerce.android.cardreader.internal

import com.stripe.stripeterminal.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.callable.ConnectionTokenProvider
import com.woocommerce.android.cardreader.CardReaderStore
import kotlinx.coroutines.runBlocking

internal class TokenProvider(private val storeWrapper: CardReaderStore) : ConnectionTokenProvider {
    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        // this method is invoked from the 3rd party Stripe SDK on a bg thread
        runBlocking { storeWrapper.getConnectionToken() }
    }
}
