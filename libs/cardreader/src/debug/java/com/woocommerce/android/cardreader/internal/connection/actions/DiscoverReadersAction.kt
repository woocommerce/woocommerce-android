package com.woocommerce.android.cardreader.internal.connection.actions

import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.model.external.DeviceType.CHIPPER_2X
import com.stripe.stripeterminal.model.external.DiscoveryConfiguration
import com.stripe.stripeterminal.model.external.Reader
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val DISCOVERY_TIMEOUT_IN_SECONDS = 60

@ExperimentalCoroutinesApi
internal class DiscoverReadersAction(private val terminal: TerminalWrapper) {
    sealed class DiscoverReadersStatus {
        object Started : DiscoverReadersStatus()
        object Success : DiscoverReadersStatus()
        data class FoundReaders(val readers: List<Reader>) : DiscoverReadersStatus()
        data class Failure(val exception: TerminalException) : DiscoverReadersStatus()
    }
    fun discoverReaders(isSimulated: Boolean): Flow<DiscoverReadersStatus> {
        return callbackFlow {
            this.sendBlocking(Started)
            val config = DiscoveryConfiguration(DISCOVERY_TIMEOUT_IN_SECONDS, CHIPPER_2X, isSimulated)
            var cancelable: Cancelable? = null
            try {
                cancelable = terminal.discoverReaders(config, object : DiscoveryListener {
                    override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                        this@callbackFlow.sendBlocking(FoundReaders(readers))
                    }
                }, object : Callback {
                    override fun onFailure(e: TerminalException) {
                        this@callbackFlow.sendBlocking(Failure(e))
                        this@callbackFlow.close()
                    }

                    override fun onSuccess() {
                        this@callbackFlow.sendBlocking(Success)
                        this@callbackFlow.close()
                    }
                })
                awaitClose()
            } finally {
                cancelable?.takeIf { !it.isCompleted }?.cancel(noopCallback)
            }
        }
    }
}

private val noopCallback = object : Callback {
    override fun onFailure(e: TerminalException) {}

    override fun onSuccess() {}
}
