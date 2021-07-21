package com.woocommerce.android.cardreader.internal.payments.actions

import com.nhaarman.mockitokotlin2.*
import com.stripe.stripeterminal.callable.PaymentIntentCallback
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.PaymentIntentParameters
import com.woocommerce.android.cardreader.PaymentInfo
import com.woocommerce.android.cardreader.internal.payments.*
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class CreatePaymentActionTest {
    private lateinit var action: CreatePaymentAction
    private val paymentIntentParametersFactory = mock<PaymentIntentParametersFactory>()
    private val terminal: TerminalWrapper = mock()
    private val paymentUtils: PaymentUtils = PaymentUtils()
    private val intentParametersBuilder = mock<PaymentIntentParameters.Builder>()

    @Before
    fun setUp() {
        action = CreatePaymentAction(paymentIntentParametersFactory, terminal, paymentUtils, mock())
        whenever(paymentIntentParametersFactory.createBuilder()).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setAmount(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setCurrency(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setReceiptEmail(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setDescription(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.build()).thenReturn(mock())

        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
    }

    @Test
    fun `when creating paymentIntent succeeds, then Success is emitted`() = runBlockingTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).first()

        assertThat(result).isExactlyInstanceOf(CreatePaymentStatus.Success::class.java)
    }

    @Test
    fun `when creating paymentIntent fails, then Failure is emitted`() = runBlockingTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).first()

        assertThat(result).isExactlyInstanceOf(CreatePaymentStatus.Failure::class.java)
    }

    @Test
    fun `when creating paymentIntent succeeds, then updated paymentIntent is returned`() = runBlockingTest {
        val updatedPaymentIntent = mock<PaymentIntent>()
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(updatedPaymentIntent)
        }

        val result = action.createPaymentIntent(createPaymentInfo()).first()

        assertThat((result as CreatePaymentStatus.Success).paymentIntent).isEqualTo(updatedPaymentIntent)
    }

    @Test
    fun `when creating paymentIntent succeeds, then flow is terminated`() = runBlockingTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when creating paymentIntent fails, then flow is terminated`() = runBlockingTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when customer email not empty, then PaymentIntent setReceiptEmail invoked`() = runBlockingTest {
        val expectedEmail = "test@test.cz"

        action.createPaymentIntent(createPaymentInfo(customerEmail = expectedEmail)).toList()

        verify(intentParametersBuilder).setReceiptEmail(expectedEmail)
    }

    @Test
    fun `when customer email is null, then PaymentIntent setReceiptEmail not invoked`() = runBlockingTest {
        action.createPaymentIntent(createPaymentInfo(customerEmail = null)).toList()

        verify(intentParametersBuilder, never()).setReceiptEmail(any())
    }

    @Test
    fun `when customer email is empty, then PaymentIntent setReceiptEmail not invoked`() = runBlockingTest {
        action.createPaymentIntent(createPaymentInfo()).toList()

        verify(intentParametersBuilder, never()).setReceiptEmail(any())
    }

    @Test
    fun `when creating payment intent, then sets payment description`() = runBlockingTest {
        val expectedDescription = "test description"

        action.createPaymentIntent(createPaymentInfo(paymentDescription = expectedDescription)).toList()

        verify(intentParametersBuilder).setDescription(expectedDescription)
    }

    @Test
    fun `when creating payment intent, then dollar amount converted to cents`() = runBlockingTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val amount = BigDecimal(1)

        action.createPaymentIntent(createPaymentInfo(amount = amount)).toList()

        verify(intentParametersBuilder).setAmount(100)
    }

    private fun createPaymentInfo(
        paymentDescription: String = "",
        orderId: Long = 1L,
        amount: BigDecimal = BigDecimal(0),
        currency: String = "USD",
        customerEmail: String? = "",
        customerName: String? = "",
        storeName: String? = "",
        siteUrl: String? = ""
    ): PaymentInfo =
        PaymentInfo(
            paymentDescription = paymentDescription,
            orderId = orderId,
            amount = amount,
            currency = currency,
            customerEmail = customerEmail,
            customerName = customerName,
            storeName = storeName,
            siteUrl = siteUrl,
        )
}
