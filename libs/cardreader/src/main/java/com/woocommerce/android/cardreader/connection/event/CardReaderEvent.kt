package com.woocommerce.android.cardreader.connection.event

sealed interface CardReaderEvent {
    object Initialisation : CardReaderEvent
}
