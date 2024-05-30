package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class WooPosCardReaderEvent(
    val cardReaderFlowParam: CardReaderFlowParam,
    val cardReaderType: CardReaderType
) : MultiLiveEvent.Event() {
    data object Connection : WooPosCardReaderEvent(
        cardReaderFlowParam = CardReaderFlowParam.WooPosConnection,
        cardReaderType = CardReaderType.EXTERNAL
    )

    data class Payment(val orderId: Long) : WooPosCardReaderEvent(
        cardReaderFlowParam = CardReaderFlowParam.PaymentOrRefund.Payment(
            orderId = orderId,
            paymentType = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.WOO_POS
        ),
        cardReaderType = CardReaderType.EXTERNAL
    )
}
