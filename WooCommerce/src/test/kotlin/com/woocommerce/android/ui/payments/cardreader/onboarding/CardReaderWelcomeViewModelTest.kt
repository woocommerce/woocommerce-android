package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.initSavedStateHandle
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
        initVM()
    }

    @Test
    fun `when screen shown, then welcome screen shown set`() {
        verify(appPrefsWrapper).setCardReaderWelcomeDialogShown()
    }

    @Test
    fun `given US country code, when user clicks on continue, then the app navigates to onboarding flow with US`() {
        initVM("US")

        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
        assertThat((viewModel.event.value as NavigateToOnboardingFlow).countryCode).isEqualTo("US")
    }

    @Test
    fun `given CA country code, when user clicks on continue, then the app navigates to onboarding flow with CA`() {
        initVM("CA")

        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
        assertThat((viewModel.event.value as NavigateToOnboardingFlow).countryCode).isEqualTo("CA")
    }

    private fun initVM(countryCode: String = "US") {
        viewModel = CardReaderWelcomeViewModel(
            CardReaderWelcomeDialogFragmentArgs(
                cardReaderFlowParam = CardReaderFlowParam.CardReadersHub,
                countryCode = countryCode
            ).initSavedStateHandle(),
            appPrefsWrapper
        )
    }
}
