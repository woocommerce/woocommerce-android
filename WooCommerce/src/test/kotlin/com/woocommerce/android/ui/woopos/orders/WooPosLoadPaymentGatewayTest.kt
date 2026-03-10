package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLoadPaymentGatewayTest : BaseUnitTest() {
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
            selectedSite = selectedSite,
            coroutineDispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `given payment gateway supports refunds, when invoke called, then returns success with gateway`() =
        runTest {
            // GIVEN
            val stripeGateway = WCGatewayModel(
                id = "stripe",
                title = "Stripe",
                description = "Pay with Stripe",
                order = 0,
                isEnabled = true,
                methodTitle = "Credit Card (Stripe)",
                methodDescription = "",
                features = listOf("refunds")
            )
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(stripeGateway)

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.isSuccess).isTrue()
            val gateway = result.getOrThrow()
            assertThat(gateway.supportsRefunds).isTrue()
            assertThat(gateway.methodTitle).isEqualTo("Credit Card (Stripe)")
            assertThat(gateway.isEnabled).isTrue()
        }

    @Test
    fun `given payment gateway does not support refunds, when invoke called, then returns success with gateway`() =
        runTest {
            // GIVEN
            val orderWithCod = testOrder.copy(paymentMethod = "cod")
            val codGateway = WCGatewayModel(
                id = "cod",
                title = "Cash on Delivery",
                description = "Pay with cash on delivery",
                order = 0,
                isEnabled = true,
                methodTitle = "Cash on Delivery",
                methodDescription = "",
                features = emptyList()
            )
            whenever(gatewayStore.getGateway(testSite, "cod")).thenReturn(codGateway)

            // WHEN
            val result = sut(orderWithCod)

            // THEN
            assertThat(result.isSuccess).isTrue()
            val gateway = result.getOrThrow()
            assertThat(gateway.supportsRefunds).isFalse()
            assertThat(gateway.methodTitle).isEqualTo("Cash on Delivery")
            assertThat(gateway.isEnabled).isTrue()
        }

    @Test
    fun `given payment gateway is disabled, when invoke called, then returns success with disabled gateway`() =
        runTest {
            // GIVEN
            val disabledGateway = WCGatewayModel(
                id = "stripe",
                title = "Stripe",
                description = "Pay with Stripe",
                order = 0,
                isEnabled = false,
                methodTitle = "Credit Card (Stripe)",
                methodDescription = "",
                features = listOf("refunds")
            )
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(disabledGateway)

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.isSuccess).isTrue()
            val gateway = result.getOrThrow()
            assertThat(gateway.methodTitle).isEqualTo("Credit Card (Stripe)")
            assertThat(gateway.supportsRefunds).isTrue()
            assertThat(gateway.isEnabled).isFalse()
        }

    @Test
    fun `given payment gateway not in DB, when fetchAllGateways succeeds, then returns success with gateway`() =
        runTest {
            // GIVEN
            val stripeGateway = WCGatewayModel(
                id = "stripe",
                title = "Stripe",
                description = "Pay with Stripe",
                order = 0,
                isEnabled = true,
                methodTitle = "Credit Card (Stripe)",
                methodDescription = "",
                features = listOf("refunds")
            )
            whenever(gatewayStore.getGateway(testSite, "stripe"))
                .thenReturn(null)
                .thenReturn(stripeGateway)
            whenever(gatewayStore.fetchAllGateways(testSite))
                .thenReturn(WooResult(listOf(stripeGateway)))

            // WHEN
            val result = sut(testOrder)

            // THEN
            verify(gatewayStore).fetchAllGateways(testSite)
            assertThat(result.isSuccess).isTrue()
            val gateway = result.getOrThrow()
            assertThat(gateway.supportsRefunds).isTrue()
            assertThat(gateway.methodTitle).isEqualTo("Credit Card (Stripe)")
        }

    @Test
    fun `given payment gateway not in DB, when fetchAllGateways fails, then returns failure`() =
        runTest {
            // GIVEN
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(null)
            whenever(gatewayStore.fetchAllGateways(testSite))
                .thenReturn(WooResult(WooError(GENERIC_ERROR, UNKNOWN)))

            // WHEN
            val result = sut(testOrder)

            // THEN
            verify(gatewayStore).fetchAllGateways(testSite)
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("Failed to fetch payment gateways")
        }

    @Test
    fun `given payment gateway not found after fetch, when invoke called, then returns failure`() =
        runTest {
            // GIVEN
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(null)
            whenever(gatewayStore.fetchAllGateways(testSite))
                .thenReturn(WooResult(emptyList()))

            // WHEN
            val result = sut(testOrder)

            // THEN
            verify(gatewayStore).fetchAllGateways(testSite)
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("Payment gateway 'stripe' not found")
        }
}
