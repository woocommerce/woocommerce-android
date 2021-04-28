package com.woocommerce.android.cardreader

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

/**
 * Interface for consumers who want to start accepting POC card payments.
 */
interface CardReaderManager {
    val isInitialized: Boolean
    val readerStatus: MutableStateFlow<CardReaderStatus>
    fun initialize(app: Application)
    fun discoverReaders(isSimulated: Boolean): Flow<CardReaderDiscoveryEvents>
    suspend fun connectToReader(cardReader: CardReader): Boolean

    // TODO cardreader Stripe accepts only Int, is that ok?
    suspend fun collectPayment(amount: BigDecimal, currency: String): Flow<CardPaymentStatus>
}
