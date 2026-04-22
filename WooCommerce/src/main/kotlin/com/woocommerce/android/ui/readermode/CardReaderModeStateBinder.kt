package com.woocommerce.android.ui.readermode

import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.remote.ExitCardReaderMode
import com.woocommerce.android.ui.payments.cardreader.payment.remote.RemoteTapToPayReaderStateBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class CardReaderModeStateBinder @Inject constructor(
    private val session: CardReaderRemoteSession,
    private val bridge: RemoteTapToPayReaderStateBridge,
) {
    fun bind(scope: CoroutineScope): Job = scope.launch {
        session.state.collect { sessionState ->
            when (sessionState) {
                CardReaderRemoteSessionState.Idle -> bridge.clear()
                is CardReaderRemoteSessionState.ReadyToPair -> bridge.push(
                    RemoteTapToPayReadyToPair(
                        deviceName = sessionState.deviceName,
                        fingerprintSuffix = sessionState.fingerprintSuffix,
                        onPrimaryActionClicked = { bridge.emitEvent(ExitCardReaderMode) },
                    )
                )
                is CardReaderRemoteSessionState.WaitingForPayment -> bridge.push(
                    RemoteTapToPayWaitingForPayment(
                        tabletName = sessionState.tabletName,
                        onPrimaryActionClicked = { bridge.emitEvent(ExitCardReaderMode) },
                    )
                )
            }
        }
    }
}
