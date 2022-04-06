package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import javax.inject.Inject

class ClearCardReaderDataAction @Inject constructor(
    private val cardReaderManager: CardReaderManager
) {
    suspend operator fun invoke() {
        if (cardReaderManager.initialized) {
            cardReaderManager.disconnectReader()
            cardReaderManager.clearCachedCredentials()
        }
    }
}
