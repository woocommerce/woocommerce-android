package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before

class CardReaderOnboardingCheckerTest : BaseUnitTest() {
    private lateinit var checker: CardReaderOnboardingChecker

    @Before
    fun setUp() {
        checker = CardReaderOnboardingChecker()
    }
}
