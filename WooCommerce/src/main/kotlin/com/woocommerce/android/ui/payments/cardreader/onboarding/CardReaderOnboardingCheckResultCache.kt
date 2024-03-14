package com.woocommerce.android.ui.payments.cardreader.onboarding

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardReaderOnboardingCheckResultCache @Inject constructor() {
    @Volatile
    var value: Result = Result.NotCached
        @Synchronized get

        @Synchronized set

    fun invalidate() {
        value = Result.NotCached
    }

    sealed class Result {
        data class Cached(val state: CardReaderOnboardingState.OnboardingCompleted) : Result()
        object NotCached : Result()
    }
}
