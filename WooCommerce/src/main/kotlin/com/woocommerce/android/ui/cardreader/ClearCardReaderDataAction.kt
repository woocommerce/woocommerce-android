package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import javax.inject.Inject

class ClearCardReaderDataAction @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    suspend operator fun invoke() {
        if (cardReaderManager.initialized) {
            cardReaderManager.disconnectReader()
            cardReaderManager.clearCachedCredentials()
        }
        appPrefsWrapper.removeLastConnectedCardReaderId()
    }
}
