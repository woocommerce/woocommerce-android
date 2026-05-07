package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class WooPosEffectiveReaderStatus {
    RemoteConnected,
    BluetoothConnected,
    Reconnecting,
    Connecting,
    Disconnected,
}

@Singleton
class WooPosEffectiveReaderStatusProvider @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val remoteReaderSession: WooPosRemoteReaderSession,
) {
    val flow: Flow<WooPosEffectiveReaderStatus> = combine(
        cardReaderFacade.readerStatus,
        remoteReaderSession.state,
    ) { bt, remote -> toEffective(bt, remote) }.distinctUntilChanged()

    fun current(): WooPosEffectiveReaderStatus = toEffective(
        cardReaderFacade.readerStatus.value,
        remoteReaderSession.state.value,
    )

    private fun toEffective(
        bt: CardReaderStatus,
        remote: WooPosRemoteReaderSession.State,
    ): WooPosEffectiveReaderStatus = when {
        remote is WooPosRemoteReaderSession.State.Connected -> WooPosEffectiveReaderStatus.RemoteConnected
        bt is CardReaderStatus.Connected -> WooPosEffectiveReaderStatus.BluetoothConnected
        bt is CardReaderStatus.Reconnecting -> WooPosEffectiveReaderStatus.Reconnecting
        bt is CardReaderStatus.Connecting || remote is WooPosRemoteReaderSession.State.Connecting ->
            WooPosEffectiveReaderStatus.Connecting
        else -> WooPosEffectiveReaderStatus.Disconnected
    }
}
