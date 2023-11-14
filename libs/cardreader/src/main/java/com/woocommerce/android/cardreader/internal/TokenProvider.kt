package com.woocommerce.android.cardreader.internal

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.woocommerce.android.cardreader.CardReaderStore
import kotlinx.coroutines.runBlocking

internal class TokenProvider(private val storeWrapper: CardReaderStore) : ConnectionTokenProvider {
    /**
     * This method is invoked from the 3rd party Stripe SDK on a bg thread
     */
    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        try {
            val token = runBlocking { storeWrapper.fetchConnectionToken() }
            callback.onSuccess(token)
        } catch (e: RuntimeException) {
            callback.onFailure(ConnectionTokenException(e.message.orEmpty(), e))
        }
    }
}
