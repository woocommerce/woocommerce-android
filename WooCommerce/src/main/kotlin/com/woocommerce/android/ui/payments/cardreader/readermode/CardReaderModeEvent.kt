package com.woocommerce.android.ui.payments.cardreader.readermode

sealed class CardReaderModeEvent {
    data object Exit : CardReaderModeEvent()
    data object RequestLocationPermission : CardReaderModeEvent()
    data object RequestLocalNetworkPermission : CardReaderModeEvent()
    data object OpenAppSettings : CardReaderModeEvent()
}
