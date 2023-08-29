package com.woocommerce.android.ui.payments.cardreader.onboarding

import javax.inject.Inject

class CardReaderOnboardingErrorClickHandler @Inject constructor() {
    operator suspend fun invoke(errorType: CardReaderOnboardingCTAErrorType): Result =
        when (errorType) {
            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED -> {

            }
        }

    enum class Result {
        SUCCESS,
    }
}

enum class CardReaderOnboardingCTAErrorType {
    WC_PAY_NOT_INSTALLED
}
