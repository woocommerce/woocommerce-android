package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class CardReaderFacade @Inject constructor(cardReaderManager: CardReaderManager) {
    val readerStatus: StateFlow<CardReaderStatus> = cardReaderManager.readerStatus

    suspend fun connectToReader() {

    }

    suspend fun collectPayment(orderId: Long) {

    }

}
