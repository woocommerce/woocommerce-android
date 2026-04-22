package com.woocommerce.android.ui.payments.cardreader.readermode

import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class CardReaderModeStateBinder @Inject constructor(
    private val session: CardReaderRemoteSession,
    private val bridge: CardReaderModeStateBridge,
) {
    fun bind(scope: CoroutineScope): Job = scope.launch {
        val exit = { bridge.emitEvent(CardReaderModeExit) }
        session.state.collect { sessionState ->
            when (sessionState) {
                CardReaderRemoteSessionState.Idle -> bridge.clear()
                CardReaderRemoteSessionState.Starting -> bridge.push(
                    RemoteTapToPayStarting(onPrimaryActionClicked = exit)
                )
                is CardReaderRemoteSessionState.ReadyToPair -> bridge.push(
                    RemoteTapToPayReadyToPair(
                        deviceName = sessionState.deviceName,
                        fingerprintSuffix = sessionState.fingerprintSuffix,
                        onPrimaryActionClicked = exit,
                    )
                )
                is CardReaderRemoteSessionState.WaitingForPayment -> bridge.push(
                    RemoteTapToPayWaitingForPayment(
                        tabletName = sessionState.tabletName,
                        onPrimaryActionClicked = exit,
                    )
                )
            }
        }
    }
}
