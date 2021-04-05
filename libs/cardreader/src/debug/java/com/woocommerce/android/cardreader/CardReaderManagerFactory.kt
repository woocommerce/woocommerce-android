package com.woocommerce.android.cardreader

import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

object CardReaderManagerFactory {
    fun createCardReaderManager(cardReaderStore: CardReaderStore): CardReaderManager =
        CardReaderManagerImpl(TerminalWrapper(), TokenProvider(cardReaderStore), LogWrapper())
}
