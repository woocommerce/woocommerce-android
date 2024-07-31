package com.woocommerce.android.ui.payments.cardreader.statuschecker

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.WOO_POS
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
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
    private val paymentsFlowTracker: PaymentsFlowTracker = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val countryCode = "US"
    private val pluginVersion = "4.0.0"

    @Test
    fun `given hub flow, when vm init, then navigates to onboarding`() = testBlocking {
        // GIVEN
        val param = CardReaderFlowParam.CardReadersHub()

        // WHEN
        val vm = initViewModel(param)

        // THEN
        assertThat(vm.event.value)
            .isEqualTo(
                CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding(
                    CardReaderOnboardingParams.Check(param),
                    CardReaderType.EXTERNAL
                )
            )
    }

    @Test
    fun `given payment flow and connected stripe m2 reader, when vm init, then navigates to payment with external`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.ExternalReader.StripeM2.name)
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow and connected stripe m2 reader with built in flow, when vm init, then disconnects and navigate to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.ExternalReader.StripeM2.name)
            }
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    pluginVersion,
                    countryCode
                )
            )

            // WHEN
            val vm = initViewModel(param, CardReaderType.BUILT_IN)

            // THEN
            verify(cardReaderManager).disconnectReader()
            verify(paymentsFlowTracker).trackAutomaticReadDisconnectWhenConnectedAnotherType()
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.BUILT_IN
                    )
                )
        }

    @Test
    fun `given payment flow and connected wisepad3 reader with built in flow, when vm init, then disconnects and navigate to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.ExternalReader.WisePade3.name)
            }
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    pluginVersion,
                    countryCode
                )
            )

            // WHEN
            val vm = initViewModel(param, CardReaderType.BUILT_IN)

            // THEN
            verify(cardReaderManager).disconnectReader()
            verify(paymentsFlowTracker).trackAutomaticReadDisconnectWhenConnectedAnotherType()
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.BUILT_IN
                    )
                )
        }

    @Test
    fun `given payment flow and connected Chipper2X reader with built in flow, when vm init, then disconnects and navigate to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.ExternalReader.Chipper2X.name)
            }
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    pluginVersion,
                    countryCode
                )
            )

            // WHEN
            val vm = initViewModel(param, CardReaderType.BUILT_IN)

            // THEN
            verify(cardReaderManager).disconnectReader()
            verify(paymentsFlowTracker).trackAutomaticReadDisconnectWhenConnectedAnotherType()
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.BUILT_IN
                    )
                )
        }

    @Test
    fun `given payment flow and connected ttp reader with external flow, when vm init, then disconnects and navigate to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.BuildInReader.CotsDevice.name)
            }
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    pluginVersion,
                    countryCode
                )
            )

            // WHEN
            val vm = initViewModel(param, CardReaderType.EXTERNAL)

            // THEN
            verify(cardReaderManager).disconnectReader()
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow and connected wisepad3 reader, when vm init, then navigates to payment with external`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.ExternalReader.WisePade3.name)
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow and connected Chipper2X reader, when vm init, then navigates to payment with external`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.ExternalReader.Chipper2X.name)
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow and connected COTS reader, when vm init, then navigates to payment with built in`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            val connectedReader: CardReader = mock {
                on { type }.thenReturn(ReaderType.BuildInReader.CotsDevice.name)
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(
                    CardReaderStatus.Connected(
                        connectedReader
                    )
                )
            )

            // WHEN
            val vm = initViewModel(param, CardReaderType.BUILT_IN)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToPayment(
                        param,
                        CardReaderType.BUILT_IN
                    )
                )
        }

    @Test
    fun `given payment flow and not connected and error, when vm init, then navigates to onboarding with fail`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingError = CardReaderOnboardingState.GenericError
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingError)

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding(
                        CardReaderOnboardingParams.Failed(param, onboardingError),
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow and not connected and onboarding success, when vm init, then navigates to welcome`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
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
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToWelcome(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow and not connected, when vm init, then navigate to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            whenever(appPrefsWrapper.isCardReaderWelcomeDialogShown()).thenReturn(true)
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
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given payment flow onboarding success welcome shown, when vm init, then navigates to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = ORDER)
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
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given woo pos connection and onboarding failed with stripe pending requirements, when vm init, then navigates to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(
                orderId = orderId,
                paymentType = WOO_POS
            )
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingState = CardReaderOnboardingState.StripeAccountPendingRequirement(
                dueDate = 0L,
                preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
                version = pluginVersion,
                countryCode = countryCode
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingState)

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isEqualTo(
                    CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection(
                        param,
                        CardReaderType.EXTERNAL
                    )
                )
        }

    @Test
    fun `given woo pos connection and onboarding failed with error other than stripe pending requirements, when vm init, then navigates to onboarding`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = WOO_POS)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingState = CardReaderOnboardingState.StripeAccountRejected(
                preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingState)

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isInstanceOf(CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToOnboarding::class.java)
        }

    @Test
    fun `given payment flow for woo pos and onboarding failed with stripe pending requirements, when vm init, then navigates to connection`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = CardReaderFlowParam.PaymentOrRefund.Payment(orderId = orderId, paymentType = WOO_POS)
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val onboardingState = CardReaderOnboardingState.StripeAccountPendingRequirement(
                dueDate = 0L,
                preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
                version = pluginVersion,
                countryCode = countryCode
            )
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(onboardingState)

            // WHEN
            val vm = initViewModel(param)

            // THEN
            assertThat(vm.event.value)
                .isInstanceOf(CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection::class.java)
        }

    private fun initViewModel(
        param: CardReaderFlowParam,
        cardReaderType: CardReaderType = CardReaderType.EXTERNAL
    ) =
        CardReaderStatusCheckerViewModel(
            CardReaderStatusCheckerDialogFragmentArgs(
                param,
                cardReaderType
            ).toSavedStateHandle(),
            cardReaderManager,
            cardReaderChecker,
            paymentsFlowTracker,
            appPrefsWrapper,
        )
}
