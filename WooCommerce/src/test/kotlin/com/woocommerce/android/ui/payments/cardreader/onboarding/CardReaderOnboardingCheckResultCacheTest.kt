package com.woocommerce.android.ui.payments.cardreader.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class CardReaderOnboardingCheckResultCacheTest {
    private val cardReaderOnboardingCheckResultCache = CardReaderOnboardingCheckResultCache()

    @Test
    fun `given cached value, when invalidate, then not cached set`() {
        // GIVEN
        cardReaderOnboardingCheckResultCache.value = CardReaderOnboardingCheckResultCache.Result.Cached(mock())

        // WHEN
        cardReaderOnboardingCheckResultCache.invalidate()

        // THEN
        assertThat(cardReaderOnboardingCheckResultCache.value).isInstanceOf(
            CardReaderOnboardingCheckResultCache.Result.NotCached::class.java
        )
    }
}
