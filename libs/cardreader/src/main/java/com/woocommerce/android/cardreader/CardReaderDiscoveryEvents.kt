package com.woocommerce.android.cardreader

sealed class CardReaderDiscoveryEvents {
    object Started : CardReaderDiscoveryEvents()
    data class ReadersFound(val list: List<CardReader>) : CardReaderDiscoveryEvents()
    data class Failed(val msg: String) : CardReaderDiscoveryEvents()
    object Succeeded : CardReaderDiscoveryEvents()
}
