package com.woocommerce.android.ui.woopos.home.totals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundableChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentStateProvider
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderTrackCanceledFlowAction
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.BackFromCheckoutToCartClicked
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent.OnBackClicked
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CreateNewOrderTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.viewmodel.ResourceProvider
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class WooPosTotalsViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val networkStatus: WooPosNetworkStatus = mock()

    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val cardReaderManager: CardReaderManager = mock()
    private val orderRepository: OrderDetailRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefs: AppPrefs = mock()
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val interacRefundableChecker: CardReaderInteracRefundableChecker = mock()
    private val tracker: PaymentsFlowTracker = mock()
    private val trackCanceledFlow = CardReaderTrackCanceledFlowAction(tracker)
    private val currencyFormatter: CurrencyFormatter = mock()
    private val errorMapper: CardReaderPaymentErrorMapper = mock()
    private val interacRefundErrorMapper: CardReaderInteracRefundErrorMapper = mock()
    private val wooStore: WooCommerceStore = mock()
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper = mock()
    private val paymentStateProvider = CardReaderPaymentStateProvider()
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper = mock()
    private val paymentReceiptHelper: PaymentReceiptHelper = mock()
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()
    private val cardReaderConfigProvider: CardReaderCountryConfigProvider = mock()
    private val paymentReceiptShare: PaymentReceiptShare = mock()
    private val uiStringParser: UiStringParser = mock()
    private val paymentControllerFactory = WooPosCardReaderPaymentControllerFactory(
        cardReaderManager = cardReaderManager,
        orderRepository = orderRepository,
        selectedSite = selectedSite,
        appPrefs = appPrefs,
        paymentCollectibilityChecker = paymentCollectibilityChecker,
        interacRefundableChecker = interacRefundableChecker,
        tracker = tracker,
        trackCancelledFlow = trackCanceledFlow,
        currencyFormatter = currencyFormatter,
        errorMapper = errorMapper,
        interacRefundErrorMapper = interacRefundErrorMapper,
        wooStore = wooStore,
        dispatchers = coroutinesTestRule.testDispatchers,
        cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
        paymentStateProvider = paymentStateProvider,
        cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
        paymentReceiptHelper = paymentReceiptHelper,
        cardReaderOnboardingChecker = cardReaderOnboardingChecker,
        cardReaderConfigProvider = cardReaderConfigProvider,
        paymentReceiptShare = paymentReceiptShare,
    )

    private fun createMockSavedStateHandle(): SavedStateHandle {
        return SavedStateHandle(
            mapOf(
                "orderId" to EMPTY_ORDER_ID,
                "totalsViewState" to WooPosTotalsViewState.Loading
            )
        )
    }

    private val cardReaderFacade: WooPosCardReaderFacade = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
    }

    @Before
    fun setUp() = runTest {
        whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderManager.batteryStatus).thenAnswer { flow { emit(CardReaderBatteryStatus.Unknown) } }
        whenever(cardReaderManager.collectPayment(any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(cardReaderManager.retryCollectPayment(any(), any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
            flow<BluetoothCardReaderMessages> {}
        }
        whenever(cardReaderFacade.readerStatus).thenAnswer { cardReaderManager.readerStatus }
    }

    @Test
    fun `initial state is loading`() = runTest {
        // GIVEN
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(MutableStateFlow(ParentToChildrenEvent.BackFromCheckoutToCartClicked))
        }
        val savedState = createMockSavedStateHandle()

        // WHEN
        val viewModel = createViewModel(
            savedState = savedState,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver
        )

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosTotalsViewState.Loading)
    }

    @Test
    @Suppress("LongMethod")
    fun `given checkout started, when vm created, then order creation is started`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
            .thenReturn("Checking order")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
            .thenReturn("Getting ready")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
            .thenReturn("Checking order")
        whenever(resourceProvider.getString(R.string.woopos_no_internet_message))
            .thenReturn("No internet")

        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            )
        )
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }

        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            id = 123L,
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                )
            ),
            productsTotal = BigDecimal("3.00"),
            total = BigDecimal("5.00"),
        )

        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(Result.success(order))
        }

        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("1.00")) }.thenReturn("$1.00")
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("$2.00")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("$3.00")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("$5.00")
        }

        // WHEN
        val viewModel = createViewModel(
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
        )

        // THEN
        val state = viewModel.state.value as WooPosTotalsViewState.Checkout
        assert(state.totals is WooPosTotalsViewState.Totals.Visible)
        val castedTotals = state.totals as WooPosTotalsViewState.Totals.Visible
        assertThat(castedTotals.orderTotalText).isEqualTo("$5.00")
        assertThat(castedTotals.orderTaxText).isEqualTo("$2.00")
        assertThat(castedTotals.orderSubtotalText).isEqualTo("$3.00")
        verify(totalsRepository).createOrderWithProducts(itemClickedData)
    }

    @Test
    fun `given checkout started and successfully created order, when vm created, then totals state correctly calculated`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
                .thenReturn("Getting ready")
            whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
                .thenReturn("Checking order")
            whenever(resourceProvider.getString(R.string.woopos_no_internet_message))
                .thenReturn("No internet")
            val itemClickedData = listOf(
                WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
            )
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val order = Order.getEmptyOrder(
                dateCreated = Date(),
                dateModified = Date()
            ).copy(
                totalTax = BigDecimal("2.00"),
                items = listOf(
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    )
                ),
                total = BigDecimal("5.00"),
                productsTotal = BigDecimal("3.00"),
            )
            val totalsRepository: WooPosTotalsRepository = mock {
                onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                    Result.success(order)
                )
            }
            val priceFormat: WooPosFormatPrice = mock {
                onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
                onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
                onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
            }

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
                priceFormat = priceFormat,
            )

            // THEN
            val state = viewModel.state.value as WooPosTotalsViewState.Checkout
            val castedTotals = state.totals as WooPosTotalsViewState.Totals.Visible
            assertThat(castedTotals.orderTotalText).isEqualTo("5.00$")
            assertThat(castedTotals.orderTaxText).isEqualTo("2.00$")
            assertThat(castedTotals.orderSubtotalText).isEqualTo("3.00$")
        }

    @Test
    fun `given OnNewTransactionClicked, should send NewTransactionClicked event and reset state to initial`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
                .thenReturn("Payment failed")
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(mock())
            }
            val savedState = createMockSavedStateHandle()
            val viewModel = createViewModel(
                savedState = savedState,
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            )

            // WHEN
            viewModel.onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked)

            // THEN
            assertThat(viewModel.state.value).isEqualTo(WooPosTotalsViewState.Loading)
            verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NewTransactionClicked)
        }

    @Test
    fun `given OnNewTransactionClicked, should track event`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
                .thenReturn("Payment failed")
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(mock())
            }
            val savedState = createMockSavedStateHandle()
            val viewModel = createViewModel(
                savedState = savedState,
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            )

            // WHEN
            viewModel.onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked)

            // THEN
            verify(analyticsTracker).track(CreateNewOrderTapped)
        }

    @Test
    fun `given order creation fails, when vm created, then error state is shown`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
            .thenReturn("Getting ready")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
            .thenReturn("Checking order")
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
        )
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val errorMessage = "Order creation failed"
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                Result.failure(Exception(errorMessage))
            )
        }

        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) }.thenReturn(errorMessage)
        }

        val savedState = createMockSavedStateHandle()

        // WHEN
        val viewModel = createViewModel(
            resourceProvider = resourceProvider,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            savedState = savedState,
        )

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosTotalsViewState.Error::class.java)
        state as WooPosTotalsViewState.Error
        assertThat(state.message).isEqualTo(errorMessage)
    }

    @Test
    fun `when RetryClicked event is triggered, should retry creating order and show loading state`() = runTest {
        // GIVEN
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
        )
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val errorMessage = "Order creation failed"
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                Result.failure(Exception(errorMessage))
            )
        }

        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) }.thenReturn(errorMessage)
        }

        val savedState = createMockSavedStateHandle()
        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("1.00")) }.thenReturn("$1.00")
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("$2.00")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("$3.00")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("$5.00")
        }

        val viewModel = createViewModel(
            resourceProvider = resourceProvider,
            savedState = savedState,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
        )

        // WHEN
        viewModel.onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked)

        // Ensure that the view model state transitions to error state
        assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.Error::class.java)

        // Mock repository to simulate success on retry
        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(subtotal = BigDecimal("1.00")),
                Order.Item.EMPTY.copy(subtotal = BigDecimal("1.00")),
                Order.Item.EMPTY.copy(subtotal = BigDecimal("1.00"))
            ),
            total = BigDecimal("5.00"),
            productsTotal = BigDecimal("3.00"),
        )

        whenever(totalsRepository.createOrderWithProducts(itemClickedData)).thenReturn(
            Result.success(order)
        )

        // Trigger RetryOrderCreationClicked again to simulate a successful retry
        viewModel.onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked)

        // Ensure the view model state transitions to the success state with correct totals
        val state = viewModel.state.value as WooPosTotalsViewState.Checkout
        val castedTotals = state.totals as WooPosTotalsViewState.Totals.Visible
        assertThat(castedTotals.orderTotalText).isEqualTo("$5.00")
        assertThat(castedTotals.orderTaxText).isEqualTo("$2.00")
        assertThat(castedTotals.orderSubtotalText).isEqualTo("$3.00")
    }

    @Test
    fun `when order is created, then should track order creation success`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
            .thenReturn("Getting ready")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
            .thenReturn("Checking order")
        whenever(resourceProvider.getString(R.string.woopos_no_internet_message))
            .thenReturn("No internet")
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
        )
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }

        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            id = 123L,
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                )
            ),
            productsTotal = BigDecimal("3.00"),
            total = BigDecimal("5.00"),
        )

        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(Result.success(order))
        }

        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
        }

        // WHEN
        val viewModel = createViewModel(
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
        )

        // THEN
        val state = viewModel.state.value as WooPosTotalsViewState.Checkout
        val castedTotals = state.totals as WooPosTotalsViewState.Totals.Visible
        assertThat(castedTotals.orderSubtotalText).isEqualTo("3.00$")
        assertThat(castedTotals.orderTaxText).isEqualTo("2.00$")
        assertThat(castedTotals.orderTotalText).isEqualTo("5.00$")
        verify(totalsRepository).createOrderWithProducts(itemClickedData)
    }

    @Test
    fun `when fails to create order, then should track order creation failure`() = runTest {
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
        )
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val errorMessage = "Order creation failed"
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                Result.failure(Exception(errorMessage))
            )
        }

        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) }.thenReturn(errorMessage)
        }

        createViewModel(
            resourceProvider = resourceProvider,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
        )

        verify(
            analyticsTracker
        ).track(
            WooPosAnalyticsEvent.Error.OrderCreationError(
                WooPosTotalsViewModel::class,
                Exception::class.java.simpleName,
                errorMessage
            )
        )
    }

    @Test
    fun `given reader connected, when order created, then payment collection started`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
            .thenReturn("Getting ready")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
            .thenReturn("Checking order")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_preparing_reader_for_payment))
            .thenReturn("Preparing reader for payment")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
            .thenReturn("Payment failed")
        whenever(resourceProvider.getString(R.string.woo_pos_payment_failed_try_again))
            .thenReturn("Try again")
        whenever(uiStringParser.asString(any())).thenReturn("Unfortunately, this payment has been declined.")

        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val paymentState =
            MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.LoadingData({})
            )
        whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

        whenever(networkStatus.isConnected()).thenReturn(true)

        // WHEN
        val viewModel = createViewModelAndSetupForSuccessfulOrderCreation(
            controllerFactory = factory
        )

        // THEN
        val state = viewModel.state.value as WooPosTotalsViewState.Checkout
        assertThat(state.readerStatus).isInstanceOf(WooPosTotalsViewState.ReaderStatus.Preparing::class.java)
    }

    @Test
    fun `given there is no internet, then collect payment method is not called`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready)).thenReturn("Getting ready")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order)).thenReturn("Checking order")
        whenever(resourceProvider.getString(R.string.woopos_no_internet_message)).thenReturn("No internet")
        whenever(networkStatus.isConnected()).thenReturn(false)
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            )
        )
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val order = createNonEmptyOrder()
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                Result.success(order)
            )
        }
        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
        }

        // WHEN
        createViewModel(
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
        )

        // THEN
        verify(cardReaderManager, never()).collectPayment(any())
    }

    @Test
    fun `given there is no internet, then parent is informed`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))

        // WHEN
        createViewModelAndSetupForSuccessfulOrderCreation()

        // THEN
        verify(cardReaderManager, never()).collectPayment(any())
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.ToastMessageDisplayed(
                message = "No internet"
            )
        )
    }

    @Test
    fun `given reader not connected, when checkout clicked, then should show error`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(false)
        val readerStatus: StateFlow<CardReaderStatus> =
            MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected())
        whenever(cardReaderManager.readerStatus).thenReturn(readerStatus)

        // WHEN
        val viewModel = createViewModelAndSetupForSuccessfulOrderCreation()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.Checkout::class.java)
        val state = viewModel.state.value as WooPosTotalsViewState.Checkout
        assertThat(state.readerStatus).isNotNull()
        with(state.readerStatus as WooPosTotalsViewState.ReaderStatus.Disconnected) {
            assertThat(title).isEqualTo("Reader not connected")
            assertThat(subtitle).isEqualTo("To process this payment, please connect your reader.")
            assertThat(actionButtonLabel).isEqualTo("Connect to reader")
        }
    }

    @Test
    fun `given reader connected, when checkout clicked, then should hide error`() = runTest {
        // WHEN
        val viewModel = createViewModelAndSetupForSuccessfulOrderCreation()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.Checkout::class.java)
        val state = viewModel.state.value as WooPosTotalsViewState.Checkout
        assertThat(state.readerStatus).isInstanceOf(WooPosTotalsViewState.ReaderStatus.Preparing::class.java)
    }

    @Test
    fun `given reader not connected, when checkout clicked and error CTA clicked, then should try connecting to reader`() =
        runTest {
            // GIVEN
            val readerStatus: StateFlow<CardReaderStatus> =
                MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected())
            whenever(cardReaderManager.readerStatus).thenReturn(readerStatus)

            // WHEN
            val viewModel = createViewModelAndSetupForSuccessfulOrderCreation()
            assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.Checkout::class.java)
            viewModel.onUIEvent(WooPosTotalsUIEvent.ConnectReaderClicked)

            // THEN
            verify(cardReaderFacade).connectToReader()
        }

    @Test
    fun `given order draft created, when reader connects, then start payment automatically`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(true)
        val readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected())
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatus)

        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)

        readerStatus.value = CardReaderStatus.Connected(mock())

        // THEN
        verify(mockCardReaderPaymentController).start()
    }

    @Test
    fun `given order draft created, when reader disconnects, then should abort payment action`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(true)
        val readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.Connected(mock()))
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatus)

        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)

        // WHEN
        readerStatus.value = CardReaderStatus.NotConnected()

        // THEN
        verify(mockCardReaderPaymentController).stop()
        verify(mockCardReaderPaymentController).onBackPressed()
    }

    @Test
    fun `given order draft created and reader connected, when card tapped, should show payment processing screen`() =
        runTest {
            // GIVEN
            whenever(
                resourceProvider.getString(
                    R.string.woopos_success_totals_payment_processing_title
                )
            ).thenReturn("Processing payment")
            whenever(
                resourceProvider.getString(
                    R.string.woopos_success_totals_payment_processing_subtitle
                )
            ).thenReturn("Please wait…")
            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)

            // WHEN
            paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
            advanceUntilIdle()

            // THEN
            assertThat(vm.state.value).isInstanceOf(WooPosTotalsViewState.PaymentInProgress::class.java)
        }

    @Test
    fun `given order draft created and reader connected, when reader is ready, should show ready for payment state`() =
        runTest {
            // GIVEN
            whenever(
                resourceProvider.getString(
                    R.string.woopos_success_totals_payment_processing_title
                )
            ).thenReturn("Processing payment")
            whenever(
                resourceProvider.getString(
                    R.string.woopos_success_totals_payment_processing_subtitle
                )
            ).thenReturn("Please wait…")
            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

            // WHEN
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)

            // THEN
            val totalState = vm.state.value as WooPosTotalsViewState.Checkout
            assertThat(totalState.readerStatus).isInstanceOf(
                WooPosTotalsViewState.ReaderStatus.ReadyForPayment::class.java
            )
        }

    @Test
    fun `given order draft created and reader connected, when payment is captured, should show processing state`() =
        runTest {
            // GIVEN
            whenever(
                resourceProvider.getString(
                    R.string.woopos_success_totals_payment_processing_title
                )
            ).thenReturn("Processing payment")
            whenever(
                resourceProvider.getString(
                    R.string.woopos_success_totals_payment_processing_subtitle
                )
            ).thenReturn("Please wait…")
            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

            // WHEN
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
            paymentState.value = CardReaderPaymentState.PaymentCapturing.ExternalReaderPaymentCapturing("")
            advanceUntilIdle()

            // THEN
            val processingState = vm.state.value as WooPosTotalsViewState.PaymentInProgress
            assertThat(processingState).isInstanceOf(WooPosTotalsViewState.PaymentInProgress::class.java)
            with(processingState) {
                assertThat(title).isEqualTo("Processing payment")
                assertThat(subtitle).isEqualTo("Please wait…")
            }
        }

    @Test
    fun `given order draft created and reader connected, when payment is processed, should show processing state`() =
        runTest {
            // GIVEN
            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

            // WHEN
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
            paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
            advanceUntilIdle()

            // THEN
            val processingState = vm.state.value as WooPosTotalsViewState.PaymentInProgress
            assertThat(processingState).isInstanceOf(WooPosTotalsViewState.PaymentInProgress::class.java)
            with(processingState) {
                assertThat(title).isEqualTo("Processing payment")
                assertThat(subtitle).isEqualTo("Please wait…")
            }
        }

    @Test
    fun `given payment failed with retry action, when retry clicked, then should retry previous payment action`() =
        runTest {
            // GIVEN
            mockPaymentFailedTexts()
            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
            paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
            val failedPaymentRetryAction: () -> Unit = mock()
            paymentState.value = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.NonCancelable(
                errorType = PaymentFlowError.NoNetwork, failedPaymentRetryAction
            )
            advanceUntilIdle()
            assertThat(vm.state.value).isInstanceOf(WooPosTotalsViewState.PaymentFailed::class.java)
            assertTrue(
                (paymentState.value as CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment).onRetry != null
            )

            // WHEN
            vm.onUIEvent(WooPosTotalsUIEvent.RetryFailedTransactionClicked)

            // THEN
            verify(failedPaymentRetryAction).invoke()
        }

    @Test
    fun `given payment failed without retry action, when retry clicked, then should cancel previous payment action and start again`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_title))
                .thenReturn("Processing payment")
            whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_subtitle))
                .thenReturn("Please wait…")
            whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
                .thenReturn("Payment failed")
            whenever(uiStringParser.asString(any())).thenReturn("Unfortunately, this payment has been declined.")
            whenever(resourceProvider.getString(R.string.woo_pos_payment_failed_try_another_payment_method))
                .thenReturn("Try another payment method")
            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
            paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
            paymentState.value = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.Cancelable(
                errorType = PaymentFlowError.NoNetwork, onRetry = null, onCancel = {}, amountWithCurrencyLabel = ""
            )
            advanceUntilIdle()
            assertThat(vm.state.value).isInstanceOf(WooPosTotalsViewState.PaymentFailed::class.java)
            assertTrue(
                (paymentState.value as CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment).onRetry == null
            )

            // WHEN
            clearInvocations(mockCardReaderPaymentController)
            vm.onUIEvent(WooPosTotalsUIEvent.RetryFailedTransactionClicked)

            // THEN
            verify(mockCardReaderPaymentController).stop()
            verify(mockCardReaderPaymentController).start()
        }

    @Test
    fun `given payment failed without retry action, when retry clicked, then should go back to checkout`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_title))
            .thenReturn("Processing payment")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_subtitle))
            .thenReturn("Please wait…")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
            .thenReturn("Payment failed")
        whenever(resourceProvider.getString(R.string.woo_pos_payment_failed_try_another_payment_method))
            .thenReturn("Try another payment method")
        whenever(uiStringParser.asString(any())).thenReturn("Unfortunately, this payment has been declined.")

        givenCardReaderConnectedAndNetworkAvailable()
        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val paymentState =
            MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
            )
        whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)
        val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
        paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
        paymentState.value = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.Cancelable(
            errorType = PaymentFlowError.NoNetwork, onRetry = null, onCancel = {}, amountWithCurrencyLabel = ""
        )
        advanceUntilIdle()

        assertThat(vm.state.value).isInstanceOf(WooPosTotalsViewState.PaymentFailed::class.java)
        assertTrue(
            (paymentState.value as CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment).onRetry == null
        )

        // WHEN
        vm.onUIEvent(WooPosTotalsUIEvent.RetryFailedTransactionClicked)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout)
    }

    @Test
    fun `given payment failed, when go back to checkout clicked, then should inform home about the situation`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
                .thenReturn("Payment failed")
            whenever(resourceProvider.getString(R.string.woo_pos_payment_failed_try_again))
                .thenReturn("Try payment again")
            whenever(uiStringParser.asString(any())).thenReturn("Unfortunately, this payment has been declined.")

            givenCardReaderConnectedAndNetworkAvailable()
            val mockCardReaderPaymentController: CardReaderPaymentController = mock()
            val factory: WooPosCardReaderPaymentControllerFactory = mock()
            whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
            val paymentState =
                MutableStateFlow<CardReaderPaymentOrRefundState>(
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
                )
            whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)
            val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
            paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
            paymentState.value = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.NonCancelable(
                errorType = PaymentFlowError.NoNetwork, {}
            )
            advanceUntilIdle()
            assertThat(vm.state.value).isInstanceOf(WooPosTotalsViewState.PaymentFailed::class.java)

            // WHEN
            vm.onUIEvent(WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedPayment)

            // THEN
            verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.GoBackToCheckoutAfterFailedPayment)
        }

    @Test
    fun `when OnStartReceiptFlowClicked is triggered, then should track event`() =
        runTest {
            // GIVEN
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(mock())
            }
            val savedState = createMockSavedStateHandle()
            val viewModel = createViewModel(
                savedState = savedState,
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            )

            // WHEN
            viewModel.onUIEvent(WooPosTotalsUIEvent.OnStartReceiptFlowClicked)

            // THEN
            verify(analyticsTracker).track(EmailReceiptTapped)
        }

    @Test
    fun `given OrderSuccessfullyPaid event arrives via parentToChildrenEventReceiver, when payment method is CARD, then PaymentSuccess state`() =
        runTest {
            // GIVEN & WHEN
            whenever(resourceProvider.getString(R.string.woopos_totals_success_payment_card, "5.00$"))
                .thenReturn("Paid 5.00$ in Card")
            val parentToChildrenEventFlow = MutableStateFlow<ParentToChildrenEvent>(
                ParentToChildrenEvent.CheckoutClicked(
                    listOf(
                        WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                            id = 1L
                        ),
                        WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                            id = 2L
                        ),
                        WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                            id = 3L
                        ),
                    )
                )
            )

            val viewModel = createViewModelAndSetupForSuccessfulOrderCreation(
                parentToChildrenEventFlow = parentToChildrenEventFlow,
            )
            parentToChildrenEventFlow.value = ParentToChildrenEvent.OrderSuccessfullyPaid(PaymentMethod.CARD)

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.PaymentSuccess::class.java)
            val successState = viewModel.state.value as WooPosTotalsViewState.PaymentSuccess
            assertThat(successState.orderTotalText).isEqualTo("Paid 5.00$ in Card")
        }

    @Test
    fun `given OrderSuccessfullyPaid event arrives via parentToChildrenEventReceiver, when payment method is CASH, then PaymentSuccess state`() =
        runTest {
            // GIVEN & WHEN
            whenever(resourceProvider.getString(R.string.woopos_totals_success_payment_cash, "5.00$"))
                .thenReturn("Paid 5.00$ in Cash")
            val parentToChildrenEventFlow = MutableStateFlow<ParentToChildrenEvent>(
                ParentToChildrenEvent.CheckoutClicked(
                    listOf(
                        WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                            id = 1L
                        ),
                        WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                            id = 2L
                        ),
                        WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                            id = 3L
                        ),
                    )
                )
            )

            val viewModel = createViewModelAndSetupForSuccessfulOrderCreation(
                parentToChildrenEventFlow = parentToChildrenEventFlow,
            )
            parentToChildrenEventFlow.value = ParentToChildrenEvent.OrderSuccessfullyPaid(PaymentMethod.CASH)

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.PaymentSuccess::class.java)
            val successState = viewModel.state.value as WooPosTotalsViewState.PaymentSuccess
            assertThat(successState.orderTotalText).isEqualTo("Paid 5.00$ in Cash")
        }

    @Test
    fun `given checkout started and order contains only free products, when vm created, then totals state correctly calculated`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
                .thenReturn("Getting ready")
            whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
                .thenReturn("Checking order")
            whenever(resourceProvider.getString(R.string.woopos_no_internet_message))
                .thenReturn("No internet")
            val itemClickedData = listOf(
                WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
            )
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val order = Order.getEmptyOrder(
                dateCreated = Date(),
                dateModified = Date()
            ).copy(
                totalTax = BigDecimal("0.00"),
                items = listOf(
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("0.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("0.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("0.00"),
                    )
                ),
                total = BigDecimal("0.00"),
                productsTotal = BigDecimal("0.00"),
            )
            val totalsRepository: WooPosTotalsRepository = mock {
                onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                    Result.success(order)
                )
            }
            val priceFormat: WooPosFormatPrice = mock {
                onBlocking { invoke(BigDecimal("0.00")) }.thenReturn("0.00$")
            }

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
                priceFormat = priceFormat,
            )

            // THEN
            viewModel.state.test {
                val checkout = awaitItem() as WooPosTotalsViewState.Checkout
                assertFalse(checkout.readerStatus is WooPosTotalsViewState.ReaderStatus.Unavailable)
            }
        }

    @Test
    fun `given checkout started and order contains non-free products, when vm created, then totals state correctly calculated`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
                .thenReturn("Getting ready")
            whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
                .thenReturn("Checking order")
            whenever(resourceProvider.getString(R.string.woopos_no_internet_message))
                .thenReturn("No internet")
            val itemClickedData = listOf(
                WooPosItemsViewModel.ItemClickedData.SimpleProduct(id = 1L)
            )
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val order = Order.getEmptyOrder(
                dateCreated = Date(),
                dateModified = Date()
            ).copy(
                totalTax = BigDecimal("2.00"),
                items = listOf(
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    )
                ),
                total = BigDecimal("5.00"),
                productsTotal = BigDecimal("3.00"),
            )
            val totalsRepository: WooPosTotalsRepository = mock {
                onBlocking { createOrderWithProducts(itemClickedData) }.thenReturn(
                    Result.success(order)
                )
            }
            val priceFormat: WooPosFormatPrice = mock {
                onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
                onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
                onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
            }

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
                priceFormat = priceFormat,
            )

            // THEN
            val checkout = viewModel.state.value as WooPosTotalsViewState.Checkout
            assertFalse(checkout.readerStatus is WooPosTotalsViewState.ReaderStatus.Unavailable)
        }

    @Test
    fun `given payment processing state, when OnBackClicked, then should ignore OnBackClicked`() = runTest {
        // GIVEN
        givenCardReaderConnectedAndNetworkAvailable()
        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val paymentState =
            MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
            )
        whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

        // WHEN
        val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
        paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
        advanceUntilIdle()

        vm.onUIEvent(OnBackClicked)

        // THEN
        verify(mockCardReaderPaymentController, never()).onBackPressed()
        verify(childrenToParentEventSender, never()).sendToParent(BackFromCheckoutToCartClicked)
        verify(childrenToParentEventSender, never()).sendToParent(ReturnedFromCardReaderPaymentToCheckout)
    }

    @Test
    fun `given payment capturing state, when OnBackClicked, then should ignore OnBackClicked`() = runTest {
        // GIVEN
        givenCardReaderConnectedAndNetworkAvailable()
        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val paymentState =
            MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
            )
        whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

        // WHEN
        val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
        paymentState.value = CardReaderPaymentState.PaymentCapturing.ExternalReaderPaymentCapturing("")
        advanceUntilIdle()

        vm.onUIEvent(OnBackClicked)

        // THEN
        verify(mockCardReaderPaymentController, never()).onBackPressed()
        verify(childrenToParentEventSender, never()).sendToParent(BackFromCheckoutToCartClicked)
        verify(childrenToParentEventSender, never()).sendToParent(ReturnedFromCardReaderPaymentToCheckout)
    }

    @Test
    fun `given payment collecting state, when OnBackClicked, then should not ignore OnBackClicked`() = runTest {
        // GIVEN
        givenCardReaderConnectedAndNetworkAvailable()
        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val paymentState =
            MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
            )
        whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

        // WHEN
        val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
        advanceUntilIdle()

        vm.onUIEvent(OnBackClicked)

        // THEN
        verify(mockCardReaderPaymentController, never()).onBackPressed()
        verify(childrenToParentEventSender).sendToParent(BackFromCheckoutToCartClicked)
    }

    @Test
    fun `given payment failed state, when OnBackClicked, then should not ignore OnBackClicked`() = runTest {
        // GIVEN
        mockPaymentFailedTexts()
        givenCardReaderConnectedAndNetworkAvailable()
        val mockCardReaderPaymentController: CardReaderPaymentController = mock()
        val factory: WooPosCardReaderPaymentControllerFactory = mock()
        whenever(factory.create(any(), any(), any())).thenReturn(mockCardReaderPaymentController)
        val paymentState =
            MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState("") {}
            )
        whenever(mockCardReaderPaymentController.paymentState).thenReturn(paymentState)

        // WHEN
        val vm = createViewModelAndSetupForSuccessfulOrderCreation(controllerFactory = factory)
        paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("") {}
        paymentState.value = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.NonCancelable(
            errorType = PaymentFlowError.NoNetwork, {}
        )
        advanceUntilIdle()

        vm.onUIEvent(OnBackClicked)

        // THEN
        verify(childrenToParentEventSender).sendToParent(ReturnedFromCardReaderPaymentToCheckout)
    }

    private fun mockPaymentFailedTexts() {
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_title))
            .thenReturn("Processing payment")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_subtitle))
            .thenReturn("Please wait…")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
            .thenReturn("Payment failed")
        whenever(uiStringParser.asString(any())).thenReturn("Unfortunately, this payment has been declined.")
        whenever(resourceProvider.getString(R.string.woo_pos_payment_failed_try_again))
            .thenReturn("Try payment again")
    }

    private fun givenCardReaderConnectedAndNetworkAvailable() {
        whenever(networkStatus.isConnected()).thenReturn(true)
        val readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.Connected(mock()))
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatus)
    }

    private fun createNonEmptyOrder() = Order.getEmptyOrder(
        dateCreated = Date(),
        dateModified = Date()
    ).copy(
        totalTax = BigDecimal("2.00"),
        items = listOf(
            Order.Item.EMPTY.copy(
                subtotal = BigDecimal("1.00"),
            ),
            Order.Item.EMPTY.copy(
                subtotal = BigDecimal("1.00"),
            ),
            Order.Item.EMPTY.copy(
                subtotal = BigDecimal("1.00"),
            )
        ),
        productsTotal = BigDecimal("3.00"),
        total = BigDecimal("5.00"),
    )

    @Suppress("LongMethod")
    private suspend fun createViewModelAndSetupForSuccessfulOrderCreation(
        controllerFactory: WooPosCardReaderPaymentControllerFactory = paymentControllerFactory,
        itemClickedData: List<WooPosItemsViewModel.ItemClickedData> = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 2L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 3L
            ),
        ),
        parentToChildrenEventFlow: MutableStateFlow<ParentToChildrenEvent> =
            MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(itemClickedData)),
    ): WooPosTotalsViewModel {
        whenever(resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_title))
            .thenReturn("Reader not connected")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_subtitle))
            .thenReturn("To process this payment, please connect your reader.")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_cta_button_label))
            .thenReturn("Connect to reader")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_getting_ready))
            .thenReturn("Getting ready")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_checking_order))
            .thenReturn("Checking order")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_title))
            .thenReturn("Processing payment")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_processing_subtitle))
            .thenReturn("Please wait…")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_ready_for_payment_title))
            .thenReturn("Ready for payment")
        whenever(resourceProvider.getString(R.string.woopos_totals_reader_ready_for_payment_subtitle))
            .thenReturn("Tap, swipe or insert card")
        whenever(resourceProvider.getString(R.string.woopos_no_internet_message))
            .thenReturn("No internet")
        whenever(resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title))
            .thenReturn("Payment failed")
        whenever(uiStringParser.asString(any())).thenReturn("Unfortunately, this payment has been declined.")

        val orderId = 23L
        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            id = orderId,
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                )
            ),
            productsTotal = BigDecimal("3.00"),
            total = BigDecimal("5.00"),
        )
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking {
                createOrderWithProducts(itemClickedData)
            }.thenReturn(Result.success(order))
        }
        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
        }
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        whenever(totalsRepository.getOrderById(orderId)).thenReturn(order)
        return createViewModel(
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            cardReaderPaymentControllerFactory = controllerFactory,
        )
    }

    private fun createViewModel(
        resourceProvider: ResourceProvider = this.resourceProvider,
        parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock(),
        totalsRepository: WooPosTotalsRepository = mock(),
        priceFormat: WooPosFormatPrice = mock(),
        savedState: SavedStateHandle = SavedStateHandle(),
        cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory = paymentControllerFactory,
    ) = WooPosTotalsViewModel(
        resourceProvider = resourceProvider,
        parentToChildrenEventReceiver = parentToChildrenEventReceiver,
        childrenToParentEventSender = childrenToParentEventSender,
        cardReaderFacade = cardReaderFacade,
        totalsRepository = totalsRepository,
        priceFormat = priceFormat,
        analyticsTracker = analyticsTracker,
        networkStatus = networkStatus,
        cardReaderPaymentControllerFactory = cardReaderPaymentControllerFactory,
        uiStringParser = uiStringParser,
        savedState = savedState,
    )
}
