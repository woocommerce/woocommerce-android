package com.woocommerce.android.ui.payments.cardreader.onboarding

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardReaderOnboardingCheckResultCache @Inject constructor() {
    @Volatile
    var value: CacheResult = CacheResult.NotCached
        @Synchronized get
        @Synchronized set

    fun invalidate() {
        value = CacheResult.NotCached
    }

    sealed class CacheResult {
        data class Cached(val state: CardReaderOnboardingState.OnboardingCompleted) : CacheResult()
        object NotCached : CacheResult()
    }
}
