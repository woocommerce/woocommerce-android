package com.woocommerce.android.cardreader

import android.app.Application
import com.woocommerce.android.cardreader.internal.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.temporary.CardReaderStoreImpl
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface for consumers who want to start accepting POC card payments.
 */
@ExperimentalCoroutinesApi
interface CardReaderManager {
    val discoveryEvents: MutableStateFlow<CardReaderDiscoveryEvents>
    fun isInitialized(): Boolean
    fun initialize(app: Application)
    fun startDiscovery(isSimulated: Boolean)
    fun connectToReader(readerId: String)
    fun onTrimMemory(level: Int)

    companion object {
        /*
         TODO cardreader This method is not using dagger since it's not initialized within this module.
          Consider refactoring this in the future.
         */
        fun createInstance(): CardReaderManager =
            CardReaderManagerImpl(TerminalWrapper(), TokenProvider(CardReaderStoreImpl()))
    }
}
