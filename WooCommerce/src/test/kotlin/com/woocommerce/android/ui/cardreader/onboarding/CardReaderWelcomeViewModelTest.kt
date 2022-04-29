package com.woocommerce.android.ui.cardreader.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderWelcomeViewModel.CardReaderWelcomeDialogEvent.NavigateToOnboardingFlow
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CardReaderWelcomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderWelcomeViewModel
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val savedState = CardReaderWelcomeDialogFragmentArgs(
        cardReaderFlowParam = CardReaderFlowParam.CardReadersHub
    ).initSavedStateHandle()

    @Before
    fun setUp() {
        viewModel = CardReaderWelcomeViewModel(savedState, appPrefsWrapper)
    }

    @Test
    fun `when screen shown, then welcome screen shown set`() {
        verify(appPrefsWrapper).setCardReaderWelcomeDialogShown()
    }

    @Test
    fun `when user clicks on continue, then the app navigates to onboarding flow`() {
        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
    }
}
