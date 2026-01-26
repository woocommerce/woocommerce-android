package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.store.WCGatewayStore

class WooPosLoadPaymentGatewayTest {
    private val gatewayStore: WCGatewayStore = mock()
    private val selectedSite: SelectedSite = mock()

    private lateinit var sut: WooPosLoadPaymentGateway

    private val testSite = SiteModel().apply { id = 1 }
    private val testOrder = OrderTestUtils.generateTestOrder().copy(paymentMethod = "stripe")

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(testSite)

        sut = WooPosLoadPaymentGateway(
            gatewayStore = gatewayStore,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `given payment gateway supports refunds, when invoke called, then returns gateway with supportsRefunds true`() =
        runTest {
            // GIVEN
            val stripeGateway: WCGatewayModel = mock {
                on { id }.thenReturn("stripe")
                on { title }.thenReturn("Stripe")
                on { description }.thenReturn("Pay with Stripe")
                on { order }.thenReturn(0)
                on { isEnabled }.thenReturn(true)
                on { methodTitle }.thenReturn("Credit Card (Stripe)")
                on { methodDescription }.thenReturn("")
                on { features }.thenReturn(listOf("refunds"))
            }
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(stripeGateway)

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.supportsRefunds).isTrue()
            assertThat(result.methodTitle).isEqualTo("Credit Card (Stripe)")
            assertThat(result.isEnabled).isTrue()
        }

    @Test
    fun `given payment gateway does not support refunds, when invoke called, then returns gateway with supportsRefunds false`() =
        runTest {
            // GIVEN
            val codGateway: WCGatewayModel = mock {
                on { id }.thenReturn("cod")
                on { title }.thenReturn("Cash on Delivery")
                on { description }.thenReturn("Pay with cash on delivery")
                on { order }.thenReturn(0)
                on { isEnabled }.thenReturn(true)
                on { methodTitle }.thenReturn("Cash on Delivery")
                on { methodDescription }.thenReturn("")
                on { features }.thenReturn(emptyList())
            }
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(codGateway)

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.supportsRefunds).isFalse()
            assertThat(result.methodTitle).isEqualTo("Cash on Delivery")
            assertThat(result.isEnabled).isTrue()
        }

    @Test
    fun `given payment gateway is disabled, when invoke called, then returns manual gateway`() =
        runTest {
            // GIVEN
            val disabledGateway: WCGatewayModel = mock {
                on { id }.thenReturn("stripe")
                on { title }.thenReturn("Stripe")
                on { description }.thenReturn("Pay with Stripe")
                on { order }.thenReturn(0)
                on { isEnabled }.thenReturn(false)
                on { methodTitle }.thenReturn("Credit Card (Stripe)")
                on { methodDescription }.thenReturn("")
                on { features }.thenReturn(listOf("refunds"))
            }
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(disabledGateway)

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.methodTitle).isEqualTo("manual")
            assertThat(result.supportsRefunds).isFalse()
        }

    @Test
    fun `given payment gateway not found, when invoke called, then returns manual gateway`() =
        runTest {
            // GIVEN
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(null)

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.methodTitle).isEqualTo("manual")
            assertThat(result.supportsRefunds).isFalse()
        }
}
