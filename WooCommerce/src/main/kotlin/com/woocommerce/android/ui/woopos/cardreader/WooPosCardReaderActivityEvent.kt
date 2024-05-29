package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class StartCardReaderConnectionFlow(
    val cardReaderFlowParam: CardReaderFlowParam,
    val cardReaderType: CardReaderType,
) : MultiLiveEvent.Event()
