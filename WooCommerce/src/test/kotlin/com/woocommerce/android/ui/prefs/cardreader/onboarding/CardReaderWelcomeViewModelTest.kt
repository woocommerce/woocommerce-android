package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderWelcomeViewModel.CardReaderWelcomeDialogEvent.NavigateToOnboardingFlow
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardReaderWelcomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderWelcomeViewModel

    @Before
    fun setUp() {
        viewModel = CardReaderWelcomeViewModel(SavedStateHandle())
    }

    @Test
    fun `when screen shown, then view state initialized`() {
        assertThat(viewModel.viewState).isNotNull
    }

    @Test
    fun `when user clicks on continue, then the app navigates to onboarding flow`() {
        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
    }
}
