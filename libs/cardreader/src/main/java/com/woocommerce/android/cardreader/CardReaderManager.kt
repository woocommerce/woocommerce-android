package com.woocommerce.android.cardreader

import android.app.Application

interface CardReaderManager {
    fun isInitialized(): Boolean
    fun initialize(app: Application)
    fun onTrimMemory(level: Int)
}
