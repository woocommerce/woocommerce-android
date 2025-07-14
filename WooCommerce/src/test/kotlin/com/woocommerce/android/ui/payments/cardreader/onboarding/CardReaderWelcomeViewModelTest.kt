package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderWelcomeViewModel.CardReaderWelcomeDialogEvent.NavigateToOnboardingFlow
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class CardReaderWelcomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderWelcomeViewModel
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    @Before
    fun setUp() {
        initVM(CardReaderType.EXTERNAL)
    }

    @Test
    fun `when screen shown, then welcome screen shown set`() {
        verify(appPrefsWrapper).setCardReaderWelcomeDialogShown()
    }

    @Test
    fun `given external card reader, when user clicks on continue, then the app navigates to onboarding flow external`() {
        initVM(CardReaderType.EXTERNAL)

        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
        assertThat((viewModel.event.value as NavigateToOnboardingFlow).cardReaderType).isEqualTo(
            CardReaderType.EXTERNAL
        )
    }

    @Test
    fun `given built in card reader, when user clicks on continue, then the app navigates to onboarding flow built in`() {
        initVM(CardReaderType.BUILT_IN)

        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
        assertThat((viewModel.event.value as NavigateToOnboardingFlow).cardReaderType).isEqualTo(
            CardReaderType.BUILT_IN
        )
    }

    private fun initVM(cardReaderType: CardReaderType) {
        viewModel = CardReaderWelcomeViewModel(
            CardReaderWelcomeDialogFragmentArgs(
                cardReaderFlowParam = CardReaderFlowParam.CardReadersHub(),
                cardReaderType = cardReaderType,
            ).toSavedStateHandle(),
            appPrefsWrapper
        )
    }
}
