package com.woocommerce.android.ui.cardreader.statuschecker

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.cardreader.onboarding.PluginType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CardReaderStatusCheckerViewModelTest : BaseUnitTest() {
    private val cardReaderManager: CardReaderManager = mock()
    private val cardReaderChecker: CardReaderOnboardingChecker = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val countryCode = "US"
    private val pluginVersion = "4.0.0"

    @Test
    fun `given hub flow, when vm init, then navigates to onboarding`() = testBlocking {
        // GIVEN
        val param = CardReaderFlowParam.CardReadersHub

        // WHEN
        val vm = initViewModel(param)

        // THEN
        assertThat(vm.event.value)
            .isEqualTo(
                CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding(
                    CardReaderOnboardingParams.Check(param)
                )
            )
    }

    @Test
    fun `given payment flow and connected reader, when vm init, then navigates to payment`() = testBlocking {
        // GIVEN
        val orderId = 1L
        val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId)
        whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))

        // WHEN
        val vm = initViewModel(param)

        // THEN
        assertThat(vm.event.value)
            .isEqualTo(CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment(param))
    }

    @Test
    fun `given payment flow and not connected and error, when vm init, then navigates to onboarding with fail`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingError = CardReaderOnboardingState.GenericError
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingError)

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding(
                        CardReaderOnboardingParams.Failed(param, onboardingError)
                    )
                )
        }

    @Test
    fun `given payment flow and not connected and onboarding success, when vm init, then navigates to welcome`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    pluginVersion,
                    countryCode
                )
            )

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToWelcome(param))
        }

    @Test
    fun `given payment flow and not connected and onboarding success, when vm init, then tracks onboarding state`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingState = CardReaderOnboardingState.OnboardingCompleted(
                PluginType.WOOCOMMERCE_PAYMENTS,
                pluginVersion,
                countryCode
            )
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingState)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackOnboardingState(onboardingState)
        }

    @Test
    fun `given payment flow onboarding success welcome shown, when vm init, then navigates to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingState = CardReaderOnboardingState.OnboardingCompleted(
                PluginType.WOOCOMMERCE_PAYMENTS,
                pluginVersion,
                countryCode
            )
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingState)

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(param))
        }

    private fun initViewModel(param: CardReaderFlowParam) =
        CardReaderStatusCheckerViewModel(
            CardReaderStatusCheckerDialogFragmentArgs(
                param
            ).initSavedStateHandle(),
            cardReaderManager,
            cardReaderChecker,
            cardReaderTracker,
            appPrefsWrapper,
        )
}
