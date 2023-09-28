package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class CardReaderOnboardingEvent : MultiLiveEvent.Event() {
    object NavigateToSupport : MultiLiveEvent.Event()

    data class NavigateToUrlInWPComWebView(val url: String) : MultiLiveEvent.Event()
    data class NavigateToUrlInGenericWebView(val url: String) : MultiLiveEvent.Event()

    data class ContinueToHub(val cardReaderFlowParam: CardReaderFlowParam) : MultiLiveEvent.Event()
    data class ContinueToConnection(
        val cardReaderFlowParam: CardReaderFlowParam,
        val cardReaderType: CardReaderType,
    ) : MultiLiveEvent.Event()
}
