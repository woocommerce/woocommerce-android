package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoImpl
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
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
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(site) }
    private val wooStore: WooCommerceStore = mock { on { getStoreCountryCode(site) }.thenReturn("US") }
    private val orderHelper: CardReaderPaymentOrderHelper = mock {
        on { getPaymentDescription(any()) }.thenReturn("Order")
    }
    private val tracker: PaymentsFlowTracker = mock()
    private val trackingInfo = CardReaderTrackingInfoImpl()
    private val appPrefs: AppPrefs = mock()
    private val flow = WooPosRemoteReaderPaymentFlow(
        cardReaderStore = store,
        remoteReaderSession = session,
        selectedSite = selectedSite,
        cardReaderPaymentOrderHelper = orderHelper,
        paymentReceiptHelper = mock(),
        wooStore = wooStore,
        resourceProvider = mock(),
        logger = mock(),
        errorMapper = mock(),
        trackingInfoKeeper = trackingInfo,
        paymentsFlowTracker = tracker,
        appPrefs = appPrefs,
    )
    private val order = Order.getEmptyOrder(Date(), Date()).copy(
        id = 123L,
        total = BigDecimal("42.50"),
        currency = "EUR",
    )

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
}
