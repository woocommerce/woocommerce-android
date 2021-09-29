package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardReaderWelcomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderWelcomeViewModel

    @Before
    fun setUp() {
        viewModel = CardReaderWelcomeViewModel(SavedStateHandle())
    }
}
