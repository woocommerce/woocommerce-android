package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.GenericErrorState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.LoadingState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.NoConnectionErrorState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.UnsupportedCountryState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCPayError
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.WCStripeError
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@ExperimentalCoroutinesApi
class CardReaderOnboardingViewModelTest : BaseUnitTest() {
    private val onboardingChecker: CardReaderOnboardingChecker = mock()

    @Test
    fun `when screen initialized, then loading state shown`() {
        val viewModel = createVM()

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingState::class.java)
    }

    @Test
    fun `when onboarding completed, then navigates to card reader hub screen`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.OnboardingCompleted)

            val viewModel = createVM()

            assertThat(viewModel.event.value)
                .isInstanceOf(CardReaderOnboardingViewModel.OnboardingEvent.NavigateToCardReaderHubFragment::class.java)
        }

    @Test
    fun `when country not supported, then country not supported state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.CountryNotSupported(""))

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedCountryState::class.java)
        }

    @Test
    fun `when country not supported, then current store country name shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.CountryNotSupported("US"))
            val viewModel = createVM()

            val countryName = (viewModel.viewStateData.value as UnsupportedCountryState).headerLabel.params[0]

            assertThat(countryName).isEqualTo(UiString.UiStringText("United States"))
        }

    @Test
    fun `given country not supported, when learn more clicked, then app shows learn more section`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.CountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedCountryState).onLearnMoreActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(CardReaderOnboardingViewModel.OnboardingEvent.ViewLearnMore::class.java)
        }

    @Test
    fun `given country not supported, when contact support clicked, then app navigates to support screen`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.CountryNotSupported(""))
            val viewModel = createVM()

            (viewModel.viewStateData.value as UnsupportedCountryState).onContactSupportActionClicked.invoke()

            assertThat(viewModel.event.value)
                .isInstanceOf(CardReaderOnboardingViewModel.OnboardingEvent.NavigateToSupport::class.java)
        }

    @Test
    fun `when wcpay not installed, then wcpay not installed state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WcpayNotInstalled)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when wcpay not activated, then wcpay not activated state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WcpayNotActivated)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotActivatedState::class.java)
        }

    @Test
    fun `when wcpay not setup, then wcpay not setup state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WcpaySetupNotCompleted)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayNotSetupState::class.java)
        }

    @Test
    fun `when unsupported wcpay version installed, then unsupported wcpay version state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WcpayUnsupportedVersion)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayError.WCPayUnsupportedVersionState::class.java)
        }

    @Test
    fun `when wcpay in test mode with live stripe account, then wcpay in test mode state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WcpayInTestModeWithLiveStripeAccount)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                WCStripeError.WCPayInTestModeWithLiveAccountState::class.java
            )
        }

    @Test
    fun `when account rejected, then account rejected state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountRejected)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCStripeError.WCPayAccountRejectedState::class.java)
        }

    @Test
    fun `when account pending requirements, then account pending requirements state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountPendingRequirement)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                WCStripeError.WCPayAccountPendingRequirementsState::class.java
            )
        }

    @Test
    fun `when account overdue requirements, then account overdue requirements state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountOverdueRequirement)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                WCStripeError.WCPayAccountOverdueRequirementsState::class.java
            )
        }

    @Test
    fun `when account under review, then account under review state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.StripeAccountUnderReview)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                WCStripeError.WCPayAccountUnderReviewState::class.java
            )
        }

    @Test
    fun `when onboarding check fails, then generic state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GenericError)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(GenericErrorState::class.java)
        }

    @Test
    fun `when network not available, then no connection error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.NoConnectionError)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(NoConnectionErrorState::class.java)
        }

    private fun createVM() = CardReaderOnboardingViewModel(SavedStateHandle(), onboardingChecker)
}
