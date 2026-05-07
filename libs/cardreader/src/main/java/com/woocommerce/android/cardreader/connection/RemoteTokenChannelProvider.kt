package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class RemoteTokenChannelProvider : ConnectionTokenProvider, AutoCloseable {
    private val tokens = Channel<String>(Channel.RENDEZVOUS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun supply(token: String) {
        tokens.send(token)
    }

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        val delivered = AtomicBoolean(false)
        val job = scope.launch {
            runCatching { tokens.receive() }
                .onSuccess { token ->
                    if (delivered.compareAndSet(false, true)) callback.onSuccess(token)
                }
                .onFailure { err ->
                    if (delivered.compareAndSet(false, true)) {
                        callback.onFailure(ConnectionTokenException(err.message.orEmpty(), err))
                    }
                }
        }
        job.invokeOnCompletion { cause ->
            if (cause != null && delivered.compareAndSet(false, true)) {
                callback.onFailure(
                    ConnectionTokenException(
                        cause.message ?: "Remote token provider is closed",
                        cause
                    )
                )
            }
        }
    }

    override fun close() {
        tokens.close()
        scope.cancel()
    }
}
