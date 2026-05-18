package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosGetPaymentMethodTest {
    private lateinit var paymentChargeRepository: PaymentChargeRepository
    private lateinit var loadPaymentGateway: WooPosLoadPaymentGateway
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var sut: WooPosGetPaymentMethod

    private val testOrder = OrderTestUtils.generateTestOrder().copy(
        paymentMethod = "stripe",
        paymentMethodTitle = "Credit card",
        chargeId = "ch_test123"
    )

    @Before
    fun setup() {
        paymentChargeRepository = mock()
        loadPaymentGateway = mock()
        resourceProvider = mock()

        whenever(resourceProvider.getString(R.string.order_refunds_manual_refund)).thenReturn("Manual refund")
        whenever(resourceProvider.getString(R.string.order_refunds_credit_card_refund))
            .thenReturn("Credit card refund")

        sut = WooPosGetPaymentMethod(
            paymentChargeRepository = paymentChargeRepository,
            loadPaymentGateway = loadPaymentGateway,
            resourceProvider = resourceProvider
        )
    }

    @Test
    fun `given enabled gateway with card details, when invoke, then returns formatted payment method with card info`() =
        runTest {
            // GIVEN
            val gateway = PaymentGateway(
                title = "Credit Card",
                description = "",
                isEnabled = true,
                methodTitle = "Credit card",
                methodDescription = "",
                supportsRefunds = true
            )
            whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "visa",
                    cardLast4 = "4242",
                    paymentMethodType = "card_present"
                )
            )

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo("Credit Card (Visa **** 4242)")
        }

    @Test
    fun `given eftpos card details, when invoke, then returns lower case eftpos`() =
        runTest {
            // GIVEN
            val gateway = PaymentGateway(
                title = "WooPayments",
                description = "",
                isEnabled = true,
                methodTitle = "WooPayments",
                methodDescription = "",
                supportsRefunds = true
            )
            whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "eftpos_au",
                    cardLast4 = "0978",
                    paymentMethodType = "card_present"
                )
            )

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo("WooPayments (eftpos **** 0978)")
        }

    @Test
    fun `given cartes bancaires card details, when invoke, then returns display name`() =
        runTest {
            // GIVEN
            val gateway = PaymentGateway(
                title = "WooPayments",
                description = "",
                isEnabled = true,
                methodTitle = "WooPayments",
                methodDescription = "",
                supportsRefunds = true
            )
            whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "cartes_bancaires",
                    cardLast4 = "1234",
                    paymentMethodType = "card_present"
                )
            )

            // WHEN
            val result = sut(testOrder)

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo("WooPayments (Cartes Bancaires **** 1234)")
        }

    @Test
    fun `given disabled gateway, when invoke, then returns manual refund with gateway title`() = runTest {
        // GIVEN
        val gateway = PaymentGateway(
            title = "Credit Card",
            description = "",
            isEnabled = false,
            methodTitle = "Credit card",
            methodDescription = "",
            supportsRefunds = false
        )
        whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
        whenever(
            resourceProvider.getString(
                R.string.order_refunds_method,
                "Manual refund",
                "Credit Card"
            )
        ).thenReturn("Manual refund (Credit Card)")

        // WHEN
        val result = sut(testOrder)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Manual refund (Credit Card)")
    }

    @Test
    fun `given gateway without refund support, when invoke, then returns manual refund`() = runTest {
        // GIVEN
        val gateway = PaymentGateway(
            title = "Credit Card",
            description = "",
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            supportsRefunds = false
        )
        whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
        whenever(
            resourceProvider.getString(
                R.string.order_refunds_method,
                "Manual refund",
                "Credit Card"
            )
        ).thenReturn("Manual refund (Credit Card)")

        // WHEN
        val result = sut(testOrder)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Manual refund (Credit Card)")
    }

    @Test
    fun `given cash payment, when invoke, then returns gateway title without card details`() = runTest {
        // GIVEN
        val cashOrder = testOrder.copy(
            paymentMethod = "cod",
            paymentMethodTitle = "Cash on delivery",
            chargeId = null
        )
        val gateway = PaymentGateway(
            title = "Cash on Delivery",
            description = "",
            isEnabled = true,
            methodTitle = "Cash on delivery",
            methodDescription = "",
            supportsRefunds = true
        )
        whenever(loadPaymentGateway.invoke(cashOrder)).thenReturn(Result.success(gateway))

        // WHEN
        val result = sut(cashOrder)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Cash on Delivery")
    }

    @Test
    fun `given order without chargeId, when invoke, then returns gateway title without card details`() = runTest {
        // GIVEN
        val orderWithoutChargeId = testOrder.copy(chargeId = null)
        val gateway = PaymentGateway(
            title = "Credit Card",
            description = "",
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            supportsRefunds = true
        )
        whenever(loadPaymentGateway.invoke(orderWithoutChargeId)).thenReturn(Result.success(gateway))

        // WHEN
        val result = sut(orderWithoutChargeId)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Credit Card")
    }

    @Test
    fun `given card fetch fails, when invoke, then returns gateway title without card details`() = runTest {
        // GIVEN
        val gateway = PaymentGateway(
            title = "Credit Card",
            description = "",
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            supportsRefunds = true
        )
        whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123"))
            .thenReturn(PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error)

        // WHEN
        val result = sut(testOrder)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Credit Card")
    }

    @Test
    fun `given gateway not found, when invoke, then returns failure`() = runTest {
        // GIVEN
        whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(
            Result.failure(Exception("Payment gateway 'stripe' not found"))
        )

        // WHEN
        val result = sut(testOrder)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Payment gateway 'stripe' not found")
    }

    @Test
    fun `given gateway with blank title, when invoke, then returns method title`() = runTest {
        // GIVEN
        val gateway = PaymentGateway(
            title = "",
            description = "",
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            supportsRefunds = true
        )
        whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "mastercard",
                cardLast4 = "5555",
                paymentMethodType = "card_present"
            )
        )

        // WHEN
        val result = sut(testOrder)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Credit card (Mastercard **** 5555)")
    }

    @Test
    fun `given card data with empty values, when invoke, then formats with empty brand and last4`() = runTest {
        // GIVEN
        val gateway = PaymentGateway(
            title = "Credit Card",
            description = "",
            isEnabled = true,
            methodTitle = "Credit card",
            methodDescription = "",
            supportsRefunds = true
        )
        whenever(loadPaymentGateway.invoke(testOrder)).thenReturn(Result.success(gateway))
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_test123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = null,
                cardLast4 = null,
                paymentMethodType = "card_present"
            )
        )

        // WHEN
        val result = sut(testOrder)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo("Credit Card ( **** )")
    }
}
