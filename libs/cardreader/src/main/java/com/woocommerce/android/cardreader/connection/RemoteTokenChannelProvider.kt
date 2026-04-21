package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RemoteTokenChannelProvider : ConnectionTokenProvider, AutoCloseable {
    private val tokens = Channel<String>(Channel.RENDEZVOUS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun supply(token: String) {
        tokens.send(token)
    }

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        if (!scope.isActive) {
            callback.onFailure(ConnectionTokenException("Remote token provider is closed"))
            return
        }
        scope.launch {
            runCatching { tokens.receive() }
                .onSuccess { callback.onSuccess(it) }
                .onFailure { err ->
                    callback.onFailure(
                        ConnectionTokenException(err.message.orEmpty(), err)
                    )
                }
        }
    }

    override fun close() {
        tokens.close()
        scope.cancel()
    }
}
