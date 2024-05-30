package com.woocommerce.android.ui.woopos.cardreader

import android.content.Context
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WooPosCardReaderFacade @Inject constructor(
    cardReaderManager: CardReaderManager,
) {
    val readerStatus: Flow<CardReaderStatus> = cardReaderManager.readerStatus

    fun connectToReader(context: Context) {
        context.startActivity(WooPosCardReaderActivity.buildIntentForCardReaderConnection(context))
    }
}
