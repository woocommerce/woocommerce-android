package com.woocommerce.android.ui.woopos.cardreader

import android.content.Context
import android.util.Log
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooPosCardReaderFacade @Inject constructor(
    private val cardReaderManager: CardReaderManager,
) {
    val readerStatus: Flow<CardReaderStatus> = cardReaderManager.readerStatus.map {
        Log.d("CardReaderFacade", "readerStatus: $it")
        it
    }

    fun connectToReader(context: Context) {
        context.startActivity(WooPosCardReaderActivity.buildIntentForCardReaderConnection(context))
    }
}
