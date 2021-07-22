package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingViewModel.OnboardingViewState.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
    fun `when onboarding completed, then flow terminated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.ONBOARDING_COMPLETED)

            val viewModel = createVM()

            assertThat(viewModel.event.value).isInstanceOf(MultiLiveEvent.Event.Exit::class.java)
        }

    @Test
    fun `when country not supported, then country not supported state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(UnsupportedCountryState::class.java)
        }

    @Test
    fun `when wcpay not installed, then wcpay not installed state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WCPAY_NOT_INSTALLED)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotInstalledState::class.java)
        }

    @Test
    fun `when wcpay not activated, then wcpay not activated state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState()).thenReturn(CardReaderOnboardingState.WCPAY_NOT_ACTIVATED)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotActivatedState::class.java)
        }

    @Test
    fun `when wcpay not setup, then wcpay not setup state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WCPAY_SETUP_NOT_COMPLETED)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayNotSetupState::class.java)
        }

    @Test
    fun `when unsupported wcpay version installed, then unsupported wcpay version state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WCPAY_UNSUPPORTED_VERSION)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayUnsupportedVersionState::class.java)
        }

    @Test
    fun `when wcpay in test mode with live stripe account, then wcpay in test mode state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.WCPAY_IN_TEST_MODE_WITH_LIVE_STRIPE_ACCOUNT)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayInTestModeWithLiveAccountState::class.java)
        }

    @Test
    fun `when account rejected, then account rejected state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayAccountRejectedState::class.java)
        }


    @Test
    fun `when account pending requirements, then account pending requirements state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.STRIPE_ACCOUNT_PENDING_REQUIREMENT)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayAccountPendingRequirementsState::class.java)
        }

    @Test
    fun `when account overdue requirements, then account overdue requirements state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.STRIPE_ACCOUNT_OVERDUE_REQUIREMENT)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayAccountOverdueRequirementsState::class.java)
        }

    @Test
    fun `when account under review, then account under review state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.STRIPE_ACCOUNT_UNDER_REVIEW)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(WCPayAccountUnderReviewState::class.java)
        }

    @Test
    fun `when onboarding check fails, then generic state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.GENERIC_ERROR)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(GenericErrorState::class.java)
        }

    @Test
    fun `when network not available, then no connection error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(onboardingChecker.getOnboardingState())
                .thenReturn(CardReaderOnboardingState.NO_CONNECTION_ERROR)

            val viewModel = createVM()

            assertThat(viewModel.viewStateData.value).isInstanceOf(NoConnectionErrorState::class.java)
        }

    private fun createVM() = CardReaderOnboardingViewModel(SavedStateHandle(), onboardingChecker)
}
