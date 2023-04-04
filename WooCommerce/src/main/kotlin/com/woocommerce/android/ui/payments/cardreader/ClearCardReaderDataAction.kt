package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import javax.inject.Inject

class ClearCardReaderDataAction @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
) {
    suspend operator fun invoke() {
        if (cardReaderManager.initialized) {
            cardReaderManager.disconnectReader()
            cardReaderManager.clearCachedCredentials()
        }
        appPrefsWrapper.removeLastConnectedCardReaderId()

        cardReaderOnboardingChecker.invalidateCache()
    }
}
