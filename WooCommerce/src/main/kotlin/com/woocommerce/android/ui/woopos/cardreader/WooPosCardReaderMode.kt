package com.woocommerce.android.ui.woopos.cardreader

import android.os.Parcelable
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCardReaderMode(
    val cardReaderFlowParam: CardReaderFlowParam,
    val cardReaderType: CardReaderType
) : Parcelable {
    @Parcelize
    data object Connection : WooPosCardReaderMode(
        cardReaderFlowParam = CardReaderFlowParam.WooPosConnection,
        cardReaderType = CardReaderType.EXTERNAL
    )

    @Parcelize
    data class Payment(val orderId: Long) : WooPosCardReaderMode(
        cardReaderFlowParam = CardReaderFlowParam.PaymentOrRefund.Payment(
            orderId = orderId,
            paymentType = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.WOO_POS
        ),
        cardReaderType = CardReaderType.EXTERNAL
    )
}
