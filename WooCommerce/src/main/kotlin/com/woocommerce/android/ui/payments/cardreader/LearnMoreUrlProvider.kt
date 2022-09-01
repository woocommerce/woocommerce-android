package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.AppUrls
import dagger.Reusable
import javax.inject.Inject

@Reusable
class LearnMoreUrlProvider @Inject constructor() {
    fun provideLearnMoreUrlFor(learnMoreUrlType: LearnMoreUrlType): String {
        return when (learnMoreUrlType) {
            LearnMoreUrlType.IN_PERSON_PAYMENTS -> {
                AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
            }
            LearnMoreUrlType.CASH_ON_DELIVERY -> {
                AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY
            }
        }
    }

    enum class LearnMoreUrlType {
        IN_PERSON_PAYMENTS,
        CASH_ON_DELIVERY
    }
}
