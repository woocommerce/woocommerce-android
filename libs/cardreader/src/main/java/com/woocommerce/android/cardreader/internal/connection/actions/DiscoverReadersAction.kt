package com.woocommerce.android.cardreader.internal.connection.actions

import androidx.annotation.RequiresPermission
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.sendAndLog
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val DISCOVERY_TIMEOUT_IN_SECONDS = 60

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

    @RequiresPermission(
        value = "android.permission.ACCESS_FINE_LOCATION",
    )
    fun discoverBuildInReaders(isSimulated: Boolean): Flow<DiscoverReadersStatus> =
        discoverReaders(DiscoveryConfiguration.LocalMobileDiscoveryConfiguration(isSimulated))

    @RequiresPermission(
        allOf = [
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN"
        ]
    )
    fun discoverExternalReaders(isSimulated: Boolean): Flow<DiscoverReadersStatus> =
        discoverReaders(
            DiscoveryConfiguration.BluetoothDiscoveryConfiguration(DISCOVERY_TIMEOUT_IN_SECONDS, isSimulated)
        )

    @RequiresPermission(
        anyOf = [
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        ],
    )
    private fun discoverReaders(config: DiscoveryConfiguration): Flow<DiscoverReadersStatus> {
        return callbackFlow {
            sendAndLog(Started, logWrapper)
            var foundReaders: List<Reader>? = null
            val cancelable = terminal.discoverReaders(
                config,
                object : DiscoveryListener {
                    override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                        if (readers != foundReaders) {
                            foundReaders = readers
                            this@callbackFlow.sendAndLog(FoundReaders(readers), logWrapper)
                        }
                    }
                },
                object : Callback {
                    override fun onFailure(e: TerminalException) {
                        this@callbackFlow.sendAndLog(Failure(e), logWrapper)
                        this@callbackFlow.close()
                    }

                    override fun onSuccess() {
                        this@callbackFlow.sendAndLog(Success, logWrapper)
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
