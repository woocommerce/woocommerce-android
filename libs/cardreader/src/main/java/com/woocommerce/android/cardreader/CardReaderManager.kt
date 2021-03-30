package com.woocommerce.android.cardreader

import android.app.Application
import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.temporary.CardReaderStoreImpl
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface for consumers who want to start accepting POC card payments.
 */
@ExperimentalCoroutinesApi
interface CardReaderManager {
    val isInitialized: Boolean
    val discoveryEvents: MutableStateFlow<CardReaderDiscoveryEvents>
    val readerStatus: MutableStateFlow<CardReaderStatus>
    fun initialize(app: Application)
    fun startDiscovery(isSimulated: Boolean)
    fun connectToReader(readerId: String)
}
