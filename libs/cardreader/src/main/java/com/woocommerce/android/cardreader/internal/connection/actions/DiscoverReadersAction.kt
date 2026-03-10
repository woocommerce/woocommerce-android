package com.woocommerce.android.cardreader.internal.connection.actions

import androidx.annotation.RequiresPermission
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

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
        discoverReaders(DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated))

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
        return terminal.discoverReaders(config)
            .distinctUntilChanged()
            .map<List<Reader>, DiscoverReadersStatus> { readers -> FoundReaders(readers) }
            .onStart { emit(Started) }
            .onCompletion { cause ->
                if (cause == null || cause is CancellationException) {
                    emit(Success)
                }
            }
            .catch { e ->
                if (e is TerminalException) {
                    emit(Failure(e))
                } else {
                    throw e
                }
            }
            .onEach { logWrapper.d(LOG_TAG, it.toString()) }
    }
}
