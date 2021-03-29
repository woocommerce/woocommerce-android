package com.woocommerce.android.cardreader

import android.app.Application

/**
 * Interface for consumers who want to start accepting POC card payments.
 */
interface CardReaderManager {
    fun isInitialized(): Boolean
    fun initialize(app: Application)
}
