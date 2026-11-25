package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.remote.CardReaderRemoteError
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.cardreader.remote.RemotePaymentCancellation
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoImpl
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRemoteReaderPaymentFlowTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val store: CardReaderStore = mock()
    private val session: WooPosRemoteReaderSession = mock {
        on { state }.thenReturn(MutableStateFlow(WooPosRemoteReaderSession.State.Idle))
    }
    private val site = SiteModel().apply {
        name = "Store"
        url = "https://example.com"
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
        on { getOrNull() }.thenReturn(site)
    }
    private val wooStore: WooCommerceStore = mock { on { getStoreCountryCode(site) }.thenReturn("US") }
    private val orderHelper: CardReaderPaymentOrderHelper = mock {
        on { getPaymentDescription(any()) }.thenReturn("Order")
    }
    private val tracker: PaymentsFlowTracker = mock()
    private val errorMapper: WooPosRemoteReaderErrorMapper = mock {
        on { toUserMessage(any(), any()) }.thenReturn("failed")
    }
    private val trackingInfo = CardReaderTrackingInfoImpl()
    private val appPrefs: AppPrefs = mock()
    private val orderRepository: com.woocommerce.android.ui.orders.details.OrderDetailRepository = mock()
    private val flow = paymentFlow()
    private fun paymentFlow(
        facade: WooPosCardReaderFacade = mock(),
        flags: FeatureFlagRepository = mock(),
    ) = WooPosRemoteReaderPaymentFlow(
        cardReaderStore = store,
        remoteReaderSession = session,
        selectedSite = selectedSite,
        cardReaderPaymentOrderHelper = orderHelper,
        paymentReceiptHelper = mock(),
        wooStore = wooStore,
        resourceProvider = mock { on { getString(any()) }.thenReturn("failed") },
        logger = mock(),
        errorMapper = errorMapper,
        trackingInfoKeeper = trackingInfo,
        paymentsFlowTracker = tracker,
        appPrefs = appPrefs,
        cardReaderFacade = facade,
        orderRepository = orderRepository,
        featureFlags = flags,
    )
    private val order = Order.getEmptyOrder(Date(), Date()).copy(
        id = 123L,
        total = BigDecimal("42.50"),
        currency = "EUR",
    )

    @Test
    fun `given unconfirmed remote outcome, when collecting payment, then confirmation timing is omitted`() = runTest {
        // GIVEN
        whenever(session.sendCollectPayment(any(), any()))
            .thenReturn(CollectPaymentOutcome.Success("pi_123", "requires_confirmation", null))
        whenever(store.capturePaymentIntent(order.id, "pi_123")).thenReturn(CapturePaymentResponse.Successful.Success)
        // WHEN
        flow.collect(order)
        // THEN
        verify(tracker, never()).onPaymentCapturing()
    }

    @Test
    fun `given remote collection succeeds, when capture succeeds, then checkout tracks success once with current order`() = runTest {
        // GIVEN
        trackingInfo.setCurrency("USD")
        trackingInfo.setPaymentMethodType("card_interac")
        trackingInfo.setCardReaderModel("STRIPE_M2")
        trackingInfo.setTransport("bluetooth")
        whenever(
            session.sendCollectPayment(any(), any())
        ).thenReturn(CollectPaymentOutcome.Success("pi_123", "requires_capture", PaymentMethodType.CARD_PRESENT))
        whenever(store.capturePaymentIntent(order.id, "pi_123")).thenReturn(CapturePaymentResponse.Successful.Success)

        doAnswer {
            assertThat(trackingInfo.trackingInfo.currency).isEqualTo("EUR")
            assertThat(trackingInfo.trackingInfo.paymentMethodType).isEqualTo("card")
            assertThat(trackingInfo.trackingInfo.cardReaderModel).isEqualTo("TAP_TO_PAY_DEVICE")
            assertThat(trackingInfo.trackingInfo.transport).isEqualTo("wifi_lan")
            null
        }.whenever(tracker).trackPaymentSucceeded(order)

        // WHEN
        val result = flow.collect(order)

        // THEN
        assertThat(result).isEqualTo(WooPosRemoteReaderPaymentFlow.Result.Completed)
        inOrder(store, tracker) {
            verify(tracker).onPaymentCapturing()
            verify(store).capturePaymentIntent(order.id, "pi_123")
            verify(tracker).trackPaymentSucceeded(order)
        }
    }

    @Test
    fun `given remote Interac collection, when capture succeeds, then tracks Interac method at checkout`() = runTest {
        // GIVEN
        whenever(session.sendCollectPayment(any(), any()))
            .thenReturn(CollectPaymentOutcome.Success("pi_123", "succeeded", PaymentMethodType.INTERAC_PRESENT))
        whenever(store.capturePaymentIntent(order.id, "pi_123")).thenReturn(CapturePaymentResponse.Successful.Success)
        doAnswer {
            assertThat(trackingInfo.trackingInfo.paymentMethodType).isEqualTo("card_interac")
            null
        }.whenever(tracker).trackPaymentSucceeded(order)

        // WHEN
        flow.collect(order)

        // THEN
        verify(tracker).trackPaymentSucceeded(order)
        verify(tracker, never()).trackInteracPaymentSucceeded()
    }

    @Test
    fun `given older phone without method, when capture succeeds, then clears stale method to unknown`() = runTest {
        // GIVEN
        trackingInfo.setPaymentMethodType("card_interac")
        whenever(session.sendCollectPayment(any(), any()))
            .thenReturn(CollectPaymentOutcome.Success("pi_123", "requires_capture", null))
        whenever(store.capturePaymentIntent(order.id, "pi_123")).thenReturn(CapturePaymentResponse.Successful.Success)
        doAnswer {
            assertThat(trackingInfo.trackingInfo.paymentMethodType).isEqualTo("unknown")
            null
        }.whenever(tracker).trackPaymentSucceeded(order)

        // WHEN
        flow.collect(order)

        // THEN
        verify(tracker).trackPaymentSucceeded(order)
        verify(tracker, never()).trackInteracPaymentSucceeded()
    }

    @Test
    fun `given remote collection succeeds, when capture fails, then success is not tracked`() = runTest {
        // GIVEN
        whenever(
            session.sendCollectPayment(any(), any())
        ).thenReturn(CollectPaymentOutcome.Success("pi_123", "requires_capture", PaymentMethodType.CARD_PRESENT))
        whenever(store.capturePaymentIntent(order.id, "pi_123"))
            .thenReturn(CapturePaymentResponse.Error.NetworkError("offline"))

        // WHEN
        val result = flow.collect(order)

        // THEN
        assertThat(result).isInstanceOf(WooPosRemoteReaderPaymentFlow.Result.Failed::class.java)
        verify(tracker, never()).trackPaymentSucceeded(any())
        verify(tracker).trackPaymentFailed("capture_failed: NetworkError - offline")
    }

    @Test
    fun `given the phone reports a canceled card read, when collecting, then canceled is tracked with the stripe code`() =
        runTest {
            // GIVEN
            whenever(session.sendCollectPayment(any(), any())).thenReturn(
                CollectPaymentOutcome.Rejected(
                    error = CardReaderRemoteError.Canceled,
                    description = "TerminalException: canceled",
                    stripeErrorCode = "CANCELED",
                )
            )
            whenever(
                errorMapper.toUserMessage(CardReaderRemoteError.Canceled, R.string.woopos_remote_payment_failed_generic)
            ).thenReturn("Transaction was canceled")

            // WHEN
            val result = flow.collect(order)

            // THEN
            assertThat(result).isEqualTo(
                WooPosRemoteReaderPaymentFlow.Result.Failed(
                    message = "Transaction was canceled",
                    errorDescription = "remote_rejected: canceled - TerminalException: canceled",
                    errorType = CardPaymentStatusErrorType.Canceled,
                    stripeErrorCode = "CANCELED",
                )
            )
            verify(tracker).trackPaymentFailed(
                errorMessage = "remote_rejected: canceled - TerminalException: canceled",
                errorType = CardPaymentStatusErrorType.Canceled,
                stripeErrorCode = "CANCELED",
            )
        }

    @Test
    fun `given the phone reports a timed out card read, when collecting, then card read timeout is tracked`() = runTest {
        // GIVEN
        whenever(session.sendCollectPayment(any(), any())).thenReturn(
            CollectPaymentOutcome.Rejected(
                error = CardReaderRemoteError.CardReadTimedOut,
                description = "TerminalException: timed out",
                stripeErrorCode = "CARD_READ_TIMED_OUT",
            )
        )

        // WHEN
        flow.collect(order)

        // THEN
        verify(tracker).trackPaymentFailed(
            errorMessage = "remote_rejected: card_read_timed_out - TerminalException: timed out",
            errorType = CardPaymentStatusErrorType.CardReadTimeOut,
            stripeErrorCode = "CARD_READ_TIMED_OUT",
        )
    }

    @Test
    fun `given an older phone reports collect failed, when collecting, then generic is tracked without a stripe code`() =
        runTest {
            // GIVEN
            whenever(session.sendCollectPayment(any(), any())).thenReturn(
                CollectPaymentOutcome.Rejected(CardReaderRemoteError.CollectFailed, "boom")
            )

            // WHEN
            flow.collect(order)

            // THEN
            verify(tracker).trackPaymentFailed(
                errorMessage = "remote_rejected: collect_failed - boom",
                errorType = CardPaymentStatusErrorType.Generic,
                stripeErrorCode = null,
            )
        }

    @Test
    fun `given a definite remote rejection, when its result arrives, then the actual facade releases collection`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        whenever(session.sendCollectPayment(any(), any())).thenReturn(
            CollectPaymentOutcome.Rejected(CardReaderRemoteError.Canceled, "cancelled", "CANCELED")
        )

        // WHEN
        flow.collect(order)

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given a remote timeout, when another collection is requested, then no second request reaches the reader`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        whenever(session.sendCollectPayment(any(), any())).thenReturn(CollectPaymentOutcome.TimedOut)
        flow.collect(order)

        // WHEN
        val result = flow.collect(order)

        // THEN
        assertThat(facade.isPaymentPending).isTrue()
        assertThat(
            (result as WooPosRemoteReaderPaymentFlow.Result.Failed).errorDescription
        ).isEqualTo("payment_unresolved")
        verify(session, times(1)).sendCollectPayment(any(), any())
    }

    @Test
    fun `given a confirmed remote cancel, when its original collection has not ended, then the owner still blocks collection`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        val reply = CompletableDeferred<CollectPaymentOutcome>()
        whenever(session.sendCollectPayment(any(), any())).doSuspendableAnswer { reply.await() }
        whenever(session.cancelPayment()).thenReturn(RemotePaymentCancellation.CONFIRMED)
        val collection = async { flow.collect(order) }
        runCurrent()
        val cancellation = async { facade.cancelPayment() }
        runCurrent()

        // THEN
        assertThat(facade.isPaymentPending).isTrue()
        assertThat(cancellation.isCompleted).isFalse()

        // WHEN
        reply.complete(CollectPaymentOutcome.Rejected(CardReaderRemoteError.Canceled, "cancelled", "CANCELED"))
        collection.await()
        cancellation.await()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given cancellation is unconfirmed, when the same remote payment succeeds, then only a drained reply releases it`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        val reply = CompletableDeferred<CollectPaymentOutcome>()
        whenever(session.sendCollectPayment(any(), any())).doSuspendableAnswer { reply.await() }
        whenever(session.cancelPayment()).thenReturn(RemotePaymentCancellation.UNCONFIRMED)
        whenever(store.capturePaymentIntent(order.id, "pi_123")).thenReturn(CapturePaymentResponse.Successful.Success)
        val collection = async { flow.collect(order) }
        runCurrent()
        facade.cancelPayment()

        // WHEN
        reply.complete(CollectPaymentOutcome.Success("pi_123", "requires_capture", PaymentMethodType.CARD_PRESENT))
        collection.await()

        // THEN
        assertThat(facade.paymentState.value)
            .isInstanceOf(WooPosCardReaderFacade.PaymentState.CancellationUnconfirmed::class.java)
        assertThat(facade.isPaymentPending).isTrue()

        // WHEN
        whenever(session.cancelPayment()).thenReturn(RemotePaymentCancellation.FAILED)
        facade.cancelPayment()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given the store changed during collection, when the old reader succeeds, then it cannot capture in the new store`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        whenever(session.sendCollectPayment(any(), any())).doSuspendableAnswer {
            whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 42 })
            CollectPaymentOutcome.Success("pi_old", "requires_capture", PaymentMethodType.CARD_PRESENT)
        }

        // WHEN
        val result = flow.collect(order)

        // THEN
        assertThat((result as WooPosRemoteReaderPaymentFlow.Result.Failed).isUnresolved).isTrue()
        assertThat(facade.isPaymentPending).isTrue()
        verify(store, never()).capturePaymentIntent(any(), any())
    }

    @Test
    fun `given PaymentAttempt preparation is suspended, when background cancels, then no remote request is sent`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(appPrefs.getCardReaderPreferredPlugin(any(), any(), any())).thenReturn(
            com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
        )
        whenever(wooStore.fetchSiteRootApiRoutes(site)).doSuspendableAnswer {
            gate.await()
            WooResult<List<String>>(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.NETWORK_ERROR))
        }
        val (flow, facade) = protectedFlow()
        val collection = async { flow.collect(order) }
        runCurrent()

        // WHEN
        val cancellation = facade.cancelPayment()
        gate.complete(Unit)
        val result = collection.await()

        // THEN
        assertThat(cancellation).isEqualTo(WooPosCardReaderFacade.CancellationResult.CANCELLED)
        assertThat((result as WooPosRemoteReaderPaymentFlow.Result.Failed).isUnresolved).isFalse()
        verify(session, never()).sendCollectPayment(any(), any())
        verify(session, never()).cancelPayment()
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given an old phone has no cancellation acknowledgement, when the original payment succeeds, then it resolves paid`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        val reply = CompletableDeferred<CollectPaymentOutcome>()
        whenever(session.sendCollectPayment(any(), any())).doSuspendableAnswer { reply.await() }
        whenever(session.cancelPayment()).thenReturn(RemotePaymentCancellation.UNSUPPORTED)
        whenever(store.capturePaymentIntent(order.id, "pi_123")).thenReturn(CapturePaymentResponse.Successful.Success)
        val collection = async { flow.collect(order) }
        runCurrent()
        val cancellation = async { facade.cancelPayment() }
        runCurrent()

        // WHEN
        reply.complete(CollectPaymentOutcome.Success("pi_123", "requires_capture", PaymentMethodType.CARD_PRESENT))
        collection.await()

        // THEN
        assertThat(cancellation.await()).isEqualTo(WooPosCardReaderFacade.CancellationResult.COMPLETED)
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given capture response was lost, when the drained reader and same order confirm payment, then receipt is restored once`() = runTest {
        // GIVEN
        val (flow, facade) = protectedFlow()
        whenever(session.sendCollectPayment(any(), any())).thenReturn(
            CollectPaymentOutcome.Success("pi_123", "requires_capture", PaymentMethodType.CARD_PRESENT)
        )
        whenever(
            store.capturePaymentIntent(order.id, "pi_123")
        ).thenReturn(CapturePaymentResponse.Error.NetworkError("offline"))
        whenever(session.cancelPayment()).thenReturn(RemotePaymentCancellation.FAILED)
        whenever(orderRepository.fetchOrderById(order.id)).thenReturn(order.copy(datePaid = Date()))
        var completed = 0
        flow.collect(order, onPaymentReconciled = { completed++ })

        // WHEN
        val result = facade.cancelPayment()
        facade.cancelPayment()

        // THEN
        assertThat(result).isEqualTo(WooPosCardReaderFacade.CancellationResult.COMPLETED)
        assertThat(completed).isEqualTo(1)
        assertThat(facade.isPaymentPending).isFalse()
        verify(store, times(1)).capturePaymentIntent(any(), any())
    }

    private fun protectedFlow(): Pair<WooPosRemoteReaderPaymentFlow, WooPosCardReaderFacade> {
        val flags: FeatureFlagRepository = mock {
            on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
        }
        val manager: CardReaderManager = mock {
            on { readerStatus }.thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        }
        val facade = WooPosCardReaderFacade(manager, flags)
        return paymentFlow(facade, flags) to facade
    }
}
