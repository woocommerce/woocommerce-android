package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoImpl
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date

class WooPosPaymentSuccessPropertiesTest {
    private val site = SiteModel()
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(site) }
    private val wooStore: WooCommerceStore = mock { on { getStoreCountryCode(site) }.thenReturn("US") }
    private val properties = WooPosPaymentSuccessProperties(PaymentUtils(mock()), selectedSite, wooStore)

    @Test
    fun `given currencies with different minor units, when payment succeeds, then gross total uses order currency`() {
        // GIVEN
        val amounts = mapOf("USD" to 1235L, "JPY" to 12L, "KWD" to 12345L)

        amounts.forEach { (currency, expectedAmount) ->
            val order = Order.getEmptyOrder(Date(), Date()).copy(
                id = 123L,
                total = BigDecimal("12.345"),
                currency = currency,
                refundTotal = BigDecimal("2.00"),
            )

            // WHEN
            val result = properties(order)

            // THEN
            assertThat(result).containsAllEntriesOf(
                mapOf(
                    "amount_normalized" to expectedAmount,
                    "currency" to currency,
                    "order_id" to 123L,
                    "country" to "US"
                )
            )
        }
    }

    @Test
    fun `given card success, when dispatched, then order properties override stale context and retain reader and gateway`() {
        // GIVEN
        val trackingInfo = CardReaderTrackingInfoImpl().apply {
            setCurrency("USD")
            setCountry("CA")
            setPaymentMethodType("card_interac")
            setCardReaderModel("STRIPE_M2")
            setTransport("bluetooth")
        }
        val wrapper: AnalyticsTrackerWrapper = mock()
        val prefs: AppPrefsWrapper = mock {
            on { getCardReaderPreferredPlugin(any(), any(), any()) }.thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
        }
        val provider = WooPosPaymentsFlowTrackerEventProvider(WooPosAnalyticsTrackingDataKeeper(), properties)
        val tracker = PaymentsFlowTracker(wrapper, prefs, selectedSite, trackingInfo, mock(), provider)
        val order = Order.getEmptyOrder(Date(), Date()).copy(id = 123L, total = BigDecimal("42.50"), currency = "EUR")

        // WHEN
        tracker.trackPaymentSucceeded(order)
        tracker.trackInteracPaymentSucceeded()

        // THEN
        val events = argumentCaptor<IAnalyticsEvent>()
        val payloads = argumentCaptor<Map<String, *>>()
        verify(wrapper, times(2)).track(events.capture(), payloads.capture())
        assertThat(events.firstValue.name).isEqualTo("card_present_collect_payment_success")
        assertThat(payloads.firstValue).containsAllEntriesOf(
            mapOf(
                "amount_normalized" to 4250L,
                "currency" to "EUR",
                "order_id" to 123L,
                "country" to "US",
                "payment_method_type" to "card_interac",
                "card_reader_model" to "STRIPE_M2",
                "transport" to "bluetooth",
                "plugin_slug" to "woocommerce-stripe",
            )
        )
        assertThat(payloads.lastValue).doesNotContainKeys("amount_normalized", "order_id")
        assertThat(events.firstValue.siteless).isFalse()
    }

    @Test
    fun `given successive orders, when success events are created, then properties stay with their order`() {
        // GIVEN
        val provider = WooPosPaymentsFlowTrackerEventProvider(WooPosAnalyticsTrackingDataKeeper(), properties)
        val firstOrder = Order.getEmptyOrder(Date(), Date()).copy(id = 1L, total = BigDecimal.TEN, currency = "USD")

        // WHEN
        val first = provider.paymentSuccessEvent(firstOrder) as WooPosAnalyticsEvent
        val second = provider.paymentSuccessEvent(
            firstOrder.copy(id = 2L, total = BigDecimal.ONE)
        ) as WooPosAnalyticsEvent

        // THEN
        assertThat(first.properties).containsEntry("order_id", 1L).containsEntry("amount_normalized", 1000L)
        assertThat(second.properties).containsEntry("order_id", 2L).containsEntry("amount_normalized", 100L)
    }
}
