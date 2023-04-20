package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class SharePaymentUrl(
    val storeName: String,
    val paymentUrl: String
) : MultiLiveEvent.Event()

data class NavigateToCardReaderHubFlow(
    val cardReaderFlowParam: CardReaderFlowParam.CardReadersHub
) : MultiLiveEvent.Event()

data class NavigateToCardReaderPaymentFlow(
    val cardReaderFlowParam: CardReaderFlowParam.PaymentOrRefund.Payment,
    val cardReaderType: CardReaderType
) : MultiLiveEvent.Event()

data class NavigateToCardReaderRefundFlow(
    val cardReaderFlowParam: CardReaderFlowParam.PaymentOrRefund.Refund,
    val cardReaderType: CardReaderType
) : MultiLiveEvent.Event()

data class NavigateBackToHub(
    val cardReaderFlowParam: CardReaderFlowParam.CardReadersHub
) : MultiLiveEvent.Event()

data class NavigateToOrderDetails(
    val orderId: Long
) : MultiLiveEvent.Event()

object NavigateToTapToPaySummary : MultiLiveEvent.Event()

object NavigateBackToOrderList : MultiLiveEvent.Event()

data class OpenGenericWebView(val url: String) : MultiLiveEvent.Event()
