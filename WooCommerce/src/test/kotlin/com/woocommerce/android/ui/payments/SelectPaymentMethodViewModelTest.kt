package com.woocommerce.android.ui.payments

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jitm.JitmState
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateBackToHub
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateBackToOrderList
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToCardReaderHubFlow
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToCardReaderRefundFlow
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToOrderDetails
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.OpenPurchaseCardReaderLink
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.ViewState.Loading
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.ViewState.Success
import com.woocommerce.android.ui.payments.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.TRY_TAP_TO_PAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Refund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.UtmProvider
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

private const val PAYMENT_URL = "paymentUrl"
private const val ORDER_TOTAL = "100$"
private const val COUNTRY_CODE = "US"

@OptIn(ExperimentalCoroutinesApi::class)
class SelectPaymentMethodViewModelTest : BaseUnitTest() {
    private val site: SiteModel = mock {
        on { name }.thenReturn("siteName")
    }
    private val order: Order = mock {
        on { paymentUrl }.thenReturn(PAYMENT_URL)
        on { total }.thenReturn(BigDecimal(1L))
    }
    private val orderEntity: OrderEntity = mock()

    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }
    private val orderStore: WCOrderStore = mock {
        onBlocking { getOrderByIdAndSite(any(), any()) }.thenReturn(orderEntity)
        on { getOrderStatusForSiteAndKey(any(), any()) }.thenReturn(mock())
    }
    private val networkStatus: NetworkStatus = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) }.thenReturn(ORDER_TOTAL)
    }
    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSiteSettings(site) }.thenReturn(mock())
    }
    private val orderMapper: OrderMapper = mock {
        on { toAppModel(orderEntity) }.thenReturn(order)
    }
    private val cardPaymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock {
        onBlocking { isCollectable(order) }.thenReturn(false)
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker = mock()
    private val selectPaymentUtmProvider: UtmProvider = mock()
    private val learnMoreUrlProvider: LearnMoreUrlProvider = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val wooStore: WooCommerceStore = mock {
        on { getStoreCountryCode(site) }.thenReturn(COUNTRY_CODE)
    }
    private val isTapToPayAvailable: IsTapToPayAvailable = mock()
    private val appPrefs: AppPrefs = mock()

    @Test
    fun `given hub flow, when view model init, then navigate to hub flow emitted`() = testBlocking {
        // GIVEN & WHEN
        val viewModel = initViewModel(CardReadersHub())

        // THEN
        assertThat(viewModel.event.value).isEqualTo(NavigateToCardReaderHubFlow(CardReadersHub()))
    }

    @Test
    fun `given hub flow, when view model init, then loading state emitted`() = testBlocking {
        // GIVEN & WHEN
        val viewModel = initViewModel(CardReadersHub())

        // THEN
        assertThat(viewModel.viewStateData.value).isEqualTo(Loading)
    }

    @Test
    fun `given refund flow, when view model init, then loading state emitted`() = testBlocking {
        // GIVEN & WHEN
        val orderId = 1L
        val refundAmount = BigDecimal(23)
        val viewModel = initViewModel(Refund(orderId, refundAmount))

        // THEN
        assertThat(viewModel.viewStateData.value).isEqualTo(Loading)
    }

    @Test
    fun `given payment flow, when view model init, then no events emitted`() = testBlocking {
        // GIVEN & WHEN
        val orderId = 1L
        val viewModel = initViewModel(Payment(orderId, ORDER))

        // THEN
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `given payment flow, when view model init, then success state emitted`() = testBlocking {
        // GIVEN
        val orderId = 1L

        // WHEN
        val viewModel = initViewModel(Payment(orderId, ORDER))

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(Success::class.java)
    }

    @Test
    fun `given payment flow and payment collectable, when view model init, then success emitted with collect true`() =
        testBlocking {
            // GIVEN & WHEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertTrue((viewModel.viewStateData.value as Success).isPaymentCollectableWithExternalCardReader)
        }

    @Test
    fun `given payment flow and ipp and ttp collectable, when view model init, then success emitted with ttp collectable true`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(IsTapToPayAvailable.Result.Available)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertTrue((viewModel.viewStateData.value as Success).isPaymentCollectableWithTapToPay)
        }

    @Test
    fun `given payment flow and ipp not collectable and ttp collectable, when view model init, then success emitted with ttp collectable false`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(false)
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(IsTapToPayAvailable.Result.Available)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertFalse((viewModel.viewStateData.value as Success).isPaymentCollectableWithTapToPay)
        }

    @Test
    fun `given payment flow and ipp collectable and ttp not collectable, when view model init, then success emitted with ttp collectable false`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(
                IsTapToPayAvailable.Result.NotAvailable.NfcNotAvailable
            )
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertFalse((viewModel.viewStateData.value as Success).isPaymentCollectableWithTapToPay)
        }

    @Test
    fun `given refund flow, when view model init, then navigate to refund flow emitted`() = testBlocking {
        // GIVEN & WHEN
        val orderId = 1L
        val refundAmount = BigDecimal(23)
        val viewModel = initViewModel(Refund(orderId, refundAmount))

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            NavigateToCardReaderRefundFlow(
                Refund(orderId, refundAmount),
                CardReaderType.EXTERNAL
            )
        )
    }

    @Test
    fun `given order payment flow, when on cash payment clicked, then show dialog event emitted`() = testBlocking {
        // GIVEN
        val orderId = 1L
        val viewModel = initViewModel(Payment(orderId, ORDER))

        // WHEN
        viewModel.onCashPaymentClicked()

        // THEN
        val events = viewModel.event.captureValues()
        assertThat(events.last()).isInstanceOf(ShowDialog::class.java)
        assertThat((events.last() as ShowDialog).titleId).isEqualTo(R.string.simple_payments_cash_dlg_title)
        assertThat((events.last() as ShowDialog).messageId).isEqualTo(R.string.existing_order_cash_dlg_message)
        assertThat((events.last() as ShowDialog).positiveButtonId).isEqualTo(R.string.simple_payments_cash_dlg_button)
        assertThat((events.last() as ShowDialog).negativeButtonId).isEqualTo(R.string.cancel)
    }

    @Test
    fun `given simple payment flow, when on cash payment clicked, then show dialog event emitted`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, SIMPLE))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            val events = viewModel.event.captureValues()
            assertThat(events.last()).isInstanceOf(ShowDialog::class.java)
            assertThat((events.last() as ShowDialog).titleId).isEqualTo(R.string.simple_payments_cash_dlg_title)
            assertThat((events.last() as ShowDialog).messageId).isEqualTo(R.string.simple_payments_cash_dlg_message)
            assertThat((events.last() as ShowDialog).positiveButtonId).isEqualTo(
                R.string.simple_payments_cash_dlg_button
            )
            assertThat((events.last() as ShowDialog).negativeButtonId).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given tap to pay test flow, when on cash payment clicked, then show dialog event emitted`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, TRY_TAP_TO_PAY))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            val events = viewModel.event.captureValues()
            assertThat(events.last()).isInstanceOf(ShowDialog::class.java)
            assertThat((events.last() as ShowDialog).titleId).isEqualTo(R.string.simple_payments_cash_dlg_title)
            assertThat((events.last() as ShowDialog).messageId).isEqualTo(R.string.simple_payments_cash_dlg_message)
            assertThat((events.last() as ShowDialog).positiveButtonId).isEqualTo(
                R.string.simple_payments_cash_dlg_button
            )
            assertThat((events.last() as ShowDialog).negativeButtonId).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given order payment flow, when on cash payment clicked, then collect tracked with order payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on cash payment clicked, then collect tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on bt reader clicked, then collect tracked with order payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onBtReaderClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                    "card_reader_type" to "external"
                )
            )
        }

    @Test
    fun `given simple payment flow, when on bt reader clicked, then collect tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onBtReaderClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                    "card_reader_type" to "external"
                )
            )
        }

    @Test
    fun `when on bt reader clicked, then navigate to card reader payment flow`() =
        testBlocking {
            // GIVEN
            val cardReaderFlowParam = Payment(1L, SIMPLE)
            val viewModel = initViewModel(cardReaderFlowParam)

            // WHEN
            viewModel.onBtReaderClicked()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                SelectPaymentMethodViewModel.NavigateToCardReaderPaymentFlow(
                    cardReaderFlowParam,
                    CardReaderType.EXTERNAL
                )
            )
        }

    @Test
    fun `when on tap too pay clicked, then navigate to card reader payment flow`() =
        testBlocking {
            // GIVEN
            val cardReaderFlowParam = Payment(1L, SIMPLE)
            val viewModel = initViewModel(cardReaderFlowParam)

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                SelectPaymentMethodViewModel.NavigateToCardReaderPaymentFlow(
                    cardReaderFlowParam,
                    CardReaderType.BUILT_IN
                )
            )
        }

    @Test
    fun `given simple payments flow, when on tap to pay clicked, then collect tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    "payment_method" to "card",
                    "flow" to "simple_payment",
                    "card_reader_type" to "built_in",
                )
            )
        }

    @Test
    fun `given order payments flow, when on tap to pay clicked, then collect tracked with order flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    "payment_method" to "card",
                    "flow" to "order_payment",
                    "card_reader_type" to "built_in",
                )
            )
        }

    @Test
    fun `when on tap too pay clicked, then app prefs stores ttp was used`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            verify(appPrefs).setTTPWasUsedAtLeastOnce()
        }

    @Test
    fun `given simple payment flow, when on connect to reader result, then failed tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onConnectToReaderResultReceived(false)

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on connect to reader result, then failed tracked with order payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onConnectToReaderResultReceived(false)

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on reader payment compl success, then complected tracked with order flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_AMOUNT to "100$",
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on reader payment compl success, then completed tracked with simple flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_AMOUNT to "100$",
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on reader payment compl fail, then fail tracked with simple flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.FAILED.value)
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on reader payment compl fail, then fail tracked with order flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.FAILED.value)
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on reader payment complete, then exit to order list`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateBackToOrderList)
        }

    @Test
    fun `given simple payment flow, when on reader payment complete, then exit to hub`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateBackToHub(CardReadersHub()))
        }

    @Test
    fun `given try ttp payment flow, when on reader payment complete, then exit to order details`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, TRY_TAP_TO_PAY))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateToOrderDetails(1))
        }

    @Test
    fun `given order payment flow, when on share link clicked, then coll tracked with order flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onSharePaymentUrlClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on share link clicked, then collect tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onSharePaymentUrlClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given tap to pay flow, when on share link clicked, then collect tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, TRY_TAP_TO_PAY))

            // WHEN
            viewModel.onSharePaymentUrlClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    AnalyticsTracker.KEY_FLOW to "tap_to_pay_try_a_payment",
                )
            )
        }

    @Test
    fun `given simple payment flow, when on share link completed, then completed tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on share link completed, then completed tracked with order flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on share link completed update fail, then fail tracked with order flow`() =
        testBlocking {
            // GIVEN
            val event = mock<OnOrderChanged> {
                on { isError }.thenReturn(true)
            }
            val error = WCOrderStore.UpdateOrderResult.RemoteUpdateResult(event)
            whenever(
                orderStore.updateOrderStatus(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(flowOf(error))
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on share link completed update fail, then fail tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val event = mock<OnOrderChanged> {
                on { isError }.thenReturn(true)
            }
            val error = WCOrderStore.UpdateOrderResult.RemoteUpdateResult(event)
            whenever(
                orderStore.updateOrderStatus(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(flowOf(error))
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on back pressed, then nothing is tracked`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onBackPressed()

            // THEN
            verify(analyticsTrackerWrapper, never()).track(eq(AnalyticsEvent.PAYMENTS_FLOW_CANCELED), any())
        }

    @Test
    fun `given order payment flow, when on back pressed, then flow cancelation is tracked`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onBackPressed()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_CANCELED,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    //region Card Reader Upsell
    @Test
    fun `given upsell banner, when purchase reader clicked, then trigger proper event`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_PAYMENTS)
            ).thenReturn(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
            )
            whenever(selectPaymentUtmProvider.getUrlWithUtmParams(any())).thenReturn(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
            )
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)

            // WHEN
            val viewModel = initViewModel(Payment(1L, ORDER))
            (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).onPrimaryActionClicked.invoke()

            // Then
            assertThat(
                viewModel.event.value
            ).isInstanceOf(OpenPurchaseCardReaderLink::class.java)
        }
    }

    @Test
    fun `given upsell banner, when banner is dismissed, then trigger DismissCardReaderUpsellBanner event`() {
        runTest {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)

            // WHEN
            val viewModel = initViewModel(Payment(1L, ORDER))
            (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).onDismissClicked.invoke()

            // Then
            assertThat(viewModel.event.value).isEqualTo(SelectPaymentMethodViewModel.DismissCardReaderUpsellBanner)
        }
    }

    @Test
    fun `given upsell banner, when banner is dismissed via remind later, then trigger proper event`() {
        // GIVEN
        val viewModel = initViewModel(Payment(1L, ORDER))

        // WHEN
        viewModel.onRemindLaterClicked(0L, KEY_BANNER_PAYMENTS)

        // Then
        assertThat(viewModel.event.value).isEqualTo(
            SelectPaymentMethodViewModel.DismissCardReaderUpsellBannerViaRemindMeLater
        )
    }

    @Test
    fun `given upsell banner, when banner is dismissed via don't show again, then trigger proper event`() {
        // GIVEN
        val viewModel = initViewModel(Payment(1L, ORDER))

        // WHEN
        viewModel.onDontShowAgainClicked(KEY_BANNER_PAYMENTS)

        // Then
        assertThat(viewModel.event.value).isEqualTo(
            SelectPaymentMethodViewModel.DismissCardReaderUpsellBannerViaDontShowAgain
        )
    }

    @Test
    fun `given card reader banner has dismissed, then update dialogShow state to true`() {
        runTest {
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)

            val viewModel = initViewModel(Payment(1L, ORDER))
            (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).onDismissClicked.invoke()

            assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isTrue
        }
    }

    @Test
    fun `given card reader banner has dismissed via remind later, then update dialogShow state to false`() {
        val viewModel = initViewModel(Payment(1L, ORDER))

        viewModel.onRemindLaterClicked(0L, KEY_BANNER_PAYMENTS)

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `given card reader banner has dismissed via don't show again, then update dialogShow state to false`() {
        val viewModel = initViewModel(Payment(1L, ORDER))

        viewModel.onDontShowAgainClicked(KEY_BANNER_PAYMENTS)

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `given view model init, then update dialogShow state to false`() {
        val viewModel = initViewModel(Payment(1L, ORDER))

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `when alert dialog dismissed by pressing back, then shouldShowUpsellCardReaderDismissDialog set to false`() {
        val viewModel = initViewModel(Payment(1L, ORDER))

        viewModel.onBannerAlertDismiss()

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `given banner displayable, when success state, then display the banner`() =
        testBlocking {
            // GIVEN & WHEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertThat(
                (viewModel.viewStateData.value as Success).bannerState
            ).isInstanceOf(JitmState.Banner::class.java)
        }

    @Test
    fun `given payment not collectable, when success state, then do not display the banner`() =
        testBlocking {
            // GIVEN & WHEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(false)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertThat(
                (viewModel.viewStateData.value as Success).bannerState
            ).isInstanceOf(JitmState.Hidden::class.java)
        }

    @Test
    fun `given banner not displayable, when success state, then do not display the banner`() =
        testBlocking {
            // GIVEN & WHEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(false)
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertThat(
                (viewModel.viewStateData.value as Success).bannerState
            ).isInstanceOf(JitmState.Hidden::class.java)
        }

    @Test
    fun `given banner displayed, when primary action invoked, then correct source is tracked`() =
        testBlocking {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_PAYMENTS)
            ).thenReturn(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
            )
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L
            val captor = argumentCaptor<String>()

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))
            (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).onPrimaryActionClicked.invoke()

            // THEN
            verify(bannerDisplayEligibilityChecker).getPurchaseCardReaderUrl(captor.capture())
            assertThat(captor.firstValue).isEqualTo(KEY_BANNER_PAYMENTS)
        }

    @Test
    fun `given banner displayed, then correct title is displayed`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))
            val title = (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).title

            // THEN
            assertThat(title).isEqualTo(
                UiString.UiStringRes(
                    stringRes = R.string.card_reader_upsell_card_reader_banner_title,
                    params = emptyList(),
                    containsHtml = false
                )
            )
        }

    @Test
    fun `given banner displayed, then correct description is displayed`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))
            val description = (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).description

            // THEN
            assertThat(description).isEqualTo(
                UiString.UiStringRes(
                    stringRes = R.string.card_reader_upsell_card_reader_banner_description,
                    params = emptyList(),
                    containsHtml = false
                )
            )
        }

    @Test
    fun `given banner displayed, then correct primary action label is displayed`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))
            val primaryActionLabel = (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).primaryActionLabel

            // THEN
            assertThat(primaryActionLabel).isEqualTo(
                UiString.UiStringRes(
                    stringRes = R.string.card_reader_upsell_card_reader_banner_cta,
                    params = emptyList(),
                    containsHtml = false
                )
            )
        }

    @Test
    fun `given banner displayed, then correct chip label is displayed`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))
            val badgeIcon = (
                (viewModel.viewStateData.value as Success).bannerState as JitmState.Banner
                ).badgeIcon

            // THEN
            assertThat(badgeIcon).isEqualTo(
                JitmState.Banner.LabelOrRemoteIcon.Label(
                    UiString.UiStringRes(stringRes = R.string.card_reader_upsell_card_reader_banner_new)
                )
            )
        }
    //endregion

    @Test
    fun `when learn more link clicked, then correct event is triggered`() {
        // GIVEN
        val viewModel = initViewModel(Payment(1L, ORDER))
        whenever(
            learnMoreUrlProvider.provideLearnMoreUrlFor(LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS)
        ).thenReturn(
            AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        )

        // WHEN
        (viewModel.viewStateData.value as Success).learMoreIpp.onClick.invoke()

        // THEN
        assertThat(viewModel.event.value).isInstanceOf(SelectPaymentMethodViewModel.OpenGenericWebView::class.java)
    }

    @Test
    fun `when learn more clicked, then trigger proper event with correct url`() {
        // GIVEN
        val viewModel = initViewModel(Payment(1L, ORDER))
        whenever(
            learnMoreUrlProvider.provideLearnMoreUrlFor(LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS)
        ).thenReturn(
            AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        )

        // WHEN
        (viewModel.viewStateData.value as Success).learMoreIpp.onClick.invoke()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            SelectPaymentMethodViewModel.OpenGenericWebView(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        )
    }

    @Test
    fun `given payment flow and not connected and ttp disabled, when vm init, then tracks tap to pay not available`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPayDisabled = IsTapToPayAvailable.Result.NotAvailable.TapToPayDisabled
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(tapToPayDisabled)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayDisabled)
        }

    @Test
    fun `given payment flow and not connected and ttp system not supported, when vm init, then tracks ttp system`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPaySystemNotSupported = IsTapToPayAvailable.Result.NotAvailable.SystemVersionNotSupported
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(tapToPaySystemNotSupported)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPaySystemNotSupported)
        }

    @Test
    fun `given payment flow and not connected and ttp country not supported, when vm init, then tracks ttp country`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPayCountryNotSupported = IsTapToPayAvailable.Result.NotAvailable.CountryNotSupported
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(tapToPayCountryNotSupported)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayCountryNotSupported)
        }

    @Test
    fun `given payment flow ttp gps not available, when vm init, then tracks ttp gps`() =
        testBlocking {
            // GIVEN
            val tapToPayGpsNotAvailable = IsTapToPayAvailable.Result.NotAvailable.GooglePlayServicesNotAvailable
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(tapToPayGpsNotAvailable)
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayGpsNotAvailable)
        }

    @Test
    fun `given payment flow and not connected and ttp nfc not available, when vm init, then tracks ttp nfc`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPayNfcNotAvailable = IsTapToPayAvailable.Result.NotAvailable.NfcNotAvailable
            whenever(isTapToPayAvailable(COUNTRY_CODE)).thenReturn(tapToPayNfcNotAvailable)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayNfcNotAvailable)
        }

    private fun initViewModel(cardReaderFlowParam: CardReaderFlowParam): SelectPaymentMethodViewModel {
        return SelectPaymentMethodViewModel(
            SelectPaymentMethodFragmentArgs(cardReaderFlowParam = cardReaderFlowParam).initSavedStateHandle(),
            selectedSite,
            orderStore,
            coroutinesTestRule.testDispatchers,
            networkStatus,
            currencyFormatter,
            wooCommerceStore,
            orderMapper,
            analyticsTrackerWrapper,
            cardPaymentCollectibilityChecker,
            bannerDisplayEligibilityChecker,
            learnMoreUrlProvider,
            cardReaderTracker,
            wooStore,
            isTapToPayAvailable,
            appPrefs,
            selectPaymentUtmProvider,
        )
    }
}
