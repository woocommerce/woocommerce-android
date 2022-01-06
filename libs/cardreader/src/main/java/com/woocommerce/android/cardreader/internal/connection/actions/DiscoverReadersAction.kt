package com.woocommerce.android.cardreader.internal.connection.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.sendAndLog
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val DISCOVERY_TIMEOUT_IN_SECONDS = 60

@ExperimentalCoroutinesApi
internal class DiscoverReadersAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper,
) {
    sealed class DiscoverReadersStatus {
        object Started : DiscoverReadersStatus()
        object Success : DiscoverReadersStatus()
        data class FoundReaders(val readers: List<Reader>) : DiscoverReadersStatus()
        data class Failure(val exception: TerminalException) : DiscoverReadersStatus()
    }

    fun discoverReaders(isSimulated: Boolean): Flow<DiscoverReadersStatus> {
        return callbackFlow {
            sendAndLog(logWrapper, Started)
            val config = DiscoveryConfiguration(
                DISCOVERY_TIMEOUT_IN_SECONDS,
                DiscoveryMethod.BLUETOOTH_SCAN,
                isSimulated,
            )
            var foundReaders: List<Reader>? = null
            val cancelable = terminal.discoverReaders(
                config,
                object : DiscoveryListener {
                    override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                        if (readers != foundReaders) {
                            foundReaders = readers
                            this@callbackFlow.sendAndLog(logWrapper, FoundReaders(readers))
                        }
                    }
                },
                object : Callback {
                    override fun onFailure(e: TerminalException) {
                        this@callbackFlow.sendAndLog(logWrapper, Failure(e))
                        this@callbackFlow.close()
                    }

                    override fun onSuccess() {
                        this@callbackFlow.sendAndLog(logWrapper, Success)
                        this@callbackFlow.close()
                    }
                }
            )
            awaitClose {
                cancelable.takeIf { !it.isCompleted }?.cancel(noopCallback)
            }
        }
    }
}

private val noopCallback = object : Callback {
    override fun onFailure(e: TerminalException) {}

    override fun onSuccess() {}
}
