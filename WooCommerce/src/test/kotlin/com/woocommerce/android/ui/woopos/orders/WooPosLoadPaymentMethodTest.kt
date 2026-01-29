package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.store.WCGatewayStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLoadPaymentMethodTest {
    private lateinit var paymentChargeRepository: PaymentChargeRepository
    private lateinit var gatewayStore: WCGatewayStore
    private lateinit var selectedSite: SelectedSite
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var coroutineDispatchers: CoroutineDispatchers
    private lateinit var sut: WooPosGetPaymentMethod

    private val testSite = SiteModel().apply { id = 1 }
    private val testOrder = OrderTestUtils.generateTestOrder().copy(
        paymentMethod = "stripe",
        paymentMethodTitle = "Credit card",
        chargeId = "ch_test123"
    )

    @Before
    fun setup() {
        paymentChargeRepository = mock()
        gatewayStore = mock()
        selectedSite = mock()
        resourceProvider = mock()
        coroutineDispatchers = CoroutineDispatchers(
            main = UnconfinedTestDispatcher(),
            computation = UnconfinedTestDispatcher(),
            io = UnconfinedTestDispatcher()
        )

        whenever(selectedSite.get()).thenReturn(testSite)
        whenever(resourceProvider.getString(R.string.order_refunds_manual_refund)).thenReturn("Manual refund")
        whenever(resourceProvider.getString(R.string.order_refunds_credit_card_refund))
            .thenReturn("Credit card refund")

        sut = WooPosGetPaymentMethod(
            paymentChargeRepository = paymentChargeRepository,
            gatewayStore = gatewayStore,
            selectedSite = selectedSite,
            resourceProvider = resourceProvider,
            coroutineDispatchers = coroutineDispatchers
        )
    }

    @Test
    fun `given enabled gateway with card details, when invoke, then returns formatted payment method with card info`() =
        runTest {
            val gateway = WCGatewayModel(
                id = "stripe",
                title = "Credit Card",
                description = "",
                order = 0,
                isEnabled = true,
                methodTitle = "Credit card",
                methodDescription = "",
                features = listOf("refunds")
            )
            whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "visa",
                    cardLast4 = "4242",
                    paymentMethodType = "card_present"
                )
            )

            val result = sut(testOrder)

            assertThat(result).isEqualTo("Credit Card (Visa **** 4242)")
        }

    @Test
    fun `given disabled gateway, when invoke, then returns manual refund with gateway title`() = runTest {
        val gateway = WCGatewayModel(
            id = "stripe",
            title = "Credit Card",
            description = "",
            order = 0,
            isEnabled = false,
            methodTitle = "Credit card",
            methodDescription = "",
            features = emptyList()
        )
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)
        whenever(
            resourceProvider.getString(
                R.string.order_refunds_method,
                "Manual refund",
                "Credit Card"
            )
        ).thenReturn("Manual refund (Credit Card)")

        val result = sut(testOrder)

        assertThat(result).isEqualTo("Manual refund (Credit Card)")
    }

    @Test
    fun `given gateway without refund support, when invoke, then returns manual refund`() = runTest {
        val gateway = WCGatewayModel(
            id = "stripe",
            title = "Credit Card",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            features = emptyList()
        )
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)
        whenever(
            resourceProvider.getString(
                R.string.order_refunds_method,
                "Manual refund",
                "Credit Card"
            )
        ).thenReturn("Manual refund (Credit Card)")

        val result = sut(testOrder)

        assertThat(result).isEqualTo("Manual refund (Credit Card)")
    }

    @Test
    fun `given cash payment, when invoke, then returns gateway title without card details`() = runTest {
        val cashOrder = testOrder.copy(
            paymentMethod = "cod",
            paymentMethodTitle = "Cash on delivery",
            chargeId = null
        )
        val gateway = WCGatewayModel(
            id = "cod",
            title = "Cash on Delivery",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "Cash on delivery",
            methodDescription = "",
            features = listOf("refunds")
        )
        whenever(gatewayStore.getGateway(testSite, "cod")).thenReturn(gateway)

        val result = sut(cashOrder)

        assertThat(result).isEqualTo("Cash on Delivery")
    }

    @Test
    fun `given order without chargeId, when invoke, then returns gateway title without card details`() = runTest {
        val orderWithoutChargeId = testOrder.copy(chargeId = null)
        val gateway = WCGatewayModel(
            id = "stripe",
            title = "Credit Card",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            features = listOf("refunds")
        )
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)

        val result = sut(orderWithoutChargeId)

        assertThat(result).isEqualTo("Credit Card")
    }

    @Test
    fun `given card fetch fails, when invoke, then returns gateway title without card details`() = runTest {
        val gateway = WCGatewayModel(
            id = "stripe",
            title = "Credit Card",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            features = listOf("refunds")
        )
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123"))
            .thenReturn(PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error)

        val result = sut(testOrder)

        assertThat(result).isEqualTo("Credit Card")
    }

    @Test
    fun `given gateway not found, when invoke, then returns manual refund`() = runTest {
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(null)

        val result = sut(testOrder)

        assertThat(result).isEqualTo("Manual refund")
    }

    @Test
    fun `given gateway with blank title, when invoke, then returns method title`() = runTest {
        val gateway = WCGatewayModel(
            id = "stripe",
            title = "",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            features = listOf("refunds")
        )
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "mastercard",
                cardLast4 = "5555",
                paymentMethodType = "card_present"
            )
        )

        val result = sut(testOrder)

        assertThat(result).isEqualTo("Credit card (Mastercard **** 5555)")
    }

    @Test
    fun `given card data with empty values, when invoke, then formats with empty brand and last4`() = runTest {
        val gateway = WCGatewayModel(
            id = "stripe",
            title = "Credit Card",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            features = listOf("refunds")
        )
        whenever(gatewayStore.getGateway(testSite, "stripe")).thenReturn(gateway)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = null,
                cardLast4 = null,
                paymentMethodType = "card_present"
            )
        )

        val result = sut(testOrder)

        assertThat(result).isEqualTo("Credit Card ( **** )")
    }
}
