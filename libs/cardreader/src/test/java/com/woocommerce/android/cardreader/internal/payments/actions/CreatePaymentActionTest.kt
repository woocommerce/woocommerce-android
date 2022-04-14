package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.internal.payments.MetaDataKeys
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.PaymentInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import java.math.BigDecimal

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class CreatePaymentActionTest {
    private lateinit var action: CreatePaymentAction
    private val paymentIntentParametersFactory = mock<PaymentIntentParametersFactory>()
    private val terminal: TerminalWrapper = mock()
    private val paymentUtils: PaymentUtils = PaymentUtils()
    private val intentParametersBuilder = mock<PaymentIntentParameters.Builder>()
    private val cardReaderConfigFactory: CardReaderConfigFactory = mock()

    @Before
    fun setUp() {
        action = CreatePaymentAction(
            paymentIntentParametersFactory,
            terminal,
            paymentUtils,
            mock(),
            cardReaderConfigFactory
        )
        whenever(paymentIntentParametersFactory.createBuilder(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setAmount(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setCurrency(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setDescription(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setMetadata(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.build()).thenReturn(mock())
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(CardReaderConfigForUSA)

        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
    }

    @Test
    fun `when creating paymentIntent succeeds, then Success is emitted`() = runTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).first()

        assertThat(result).isExactlyInstanceOf(CreatePaymentStatus.Success::class.java)
    }

    @Test
    fun `when creating paymentIntent fails, then Failure is emitted`() = runTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).first()

        assertThat(result).isExactlyInstanceOf(CreatePaymentStatus.Failure::class.java)
    }

    @Test
    fun `when creating paymentIntent succeeds, then updated paymentIntent is returned`() = runTest {
        val updatedPaymentIntent = mock<PaymentIntent>()
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(updatedPaymentIntent)
        }

        val result = action.createPaymentIntent(createPaymentInfo()).first()

        assertThat((result as CreatePaymentStatus.Success).paymentIntent).isEqualTo(updatedPaymentIntent)
    }

    @Test
    fun `when creating paymentIntent succeeds, then flow is terminated`() = runTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when creating paymentIntent fails, then flow is terminated`() = runTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
        }

        val result = action.createPaymentIntent(createPaymentInfo()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `given wcpay can send emails, when customer email not empty, then PaymentIntent setReceiptEmail not invoked`() =
        runTest {
            val expectedEmail = "test@test.cz"

            action.createPaymentIntent(
                createPaymentInfo(
                    customerEmail = expectedEmail,
                    wcpayCanSendReceipt = true
                )
            ).toList()

            verify(intentParametersBuilder, never()).setReceiptEmail(any())
        }

    @Test
    fun `given wcpay can not send emails, when customer email not empty, then PaymentIntent setReceiptEmail invoked`() =
        runTest {
            val expectedEmail = "test@test.cz"

            action.createPaymentIntent(
                createPaymentInfo(
                    customerEmail = expectedEmail,
                    wcpayCanSendReceipt = false
                )
            ).toList()

            verify(intentParametersBuilder).setReceiptEmail(expectedEmail)
        }

    @Test
    fun `when customer email is null, then PaymentIntent setReceiptEmail not invoked`() = runTest {
        action.createPaymentIntent(createPaymentInfo(customerEmail = null)).toList()

        verify(intentParametersBuilder, never()).setReceiptEmail(any())
    }

    @Test
    fun `when customer email is empty, then PaymentIntent setReceiptEmail not invoked`() = runTest {
        action.createPaymentIntent(createPaymentInfo()).toList()

        verify(intentParametersBuilder, never()).setReceiptEmail(any())
    }

    @Test
    fun `when statement descriptor not empty, then PaymentIntent setStatementDescriptor invoked`() = runTest {
        val expected = "Site abcd"

        action.createPaymentIntent(createPaymentInfo(statementDescriptor = expected)).toList()

        verify(intentParametersBuilder).setStatementDescriptor(expected)
    }

    @Test
    fun `when statement descriptor "", then PaymentIntent setStatementDescriptor NOT invoked`() = runTest {
        val expected = ""

        action.createPaymentIntent(createPaymentInfo(statementDescriptor = expected)).toList()

        verify(intentParametersBuilder, never()).setStatementDescriptor(any())
    }

    @Test
    fun `when statement descriptor null, then PaymentIntent setStatementDescriptor NOT invoked`() = runTest {
        val expected: String? = null

        action.createPaymentIntent(createPaymentInfo(statementDescriptor = expected)).toList()

        verify(intentParametersBuilder, never()).setStatementDescriptor(any())
    }

    @Test
    fun `when creating payment intent, then payment description set`() = runTest {
        val expectedDescription = "test description"

        action.createPaymentIntent(createPaymentInfo(paymentDescription = expectedDescription)).toList()

        verify(intentParametersBuilder).setDescription(expectedDescription)
    }

    @Test
    fun `when creating payment intent, then store name set`() = runTest {
        val expected = "dummy store name"
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(storeName = expected)).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.STORE.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then customer name set`() = runTest {
        val expected = "dummy customer name"
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(customerName = expected)).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.CUSTOMER_NAME.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then customer email set`() = runTest {
        val expected = "dummy customer email"
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(customerEmail = expected)).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.CUSTOMER_EMAIL.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then site url set`() = runTest {
        val expected = "dummy site url"
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(siteUrl = expected)).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.SITE_URL.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then order id set`() = runTest {
        val expected = 99L
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(orderId = expected)).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.ORDER_ID.key]).isEqualTo(expected.toString())
    }

    @Test
    fun `when creating payment intent, then reader id set`() = runTest {
        val readerId = "SM12345678"
        val captor = argumentCaptor<Map<String, String>>()
        val reader = mock<CardReader> { on { id }.thenReturn(readerId) }
        whenever(terminal.getConnectedReader()).thenReturn(reader)

        action.createPaymentIntent(createPaymentInfo()).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.READER_ID.key]).isEqualTo(readerId)
    }

    @Test
    fun `given connected reader, when creating payment intent, then reader model set`() = runTest {
        val readerModel = "STRIPE_M2"
        val captor = argumentCaptor<Map<String, String>>()
        val reader = mock<CardReader> { on { type }.thenReturn(readerModel) }
        whenever(terminal.getConnectedReader()).thenReturn(reader)

        action.createPaymentIntent(createPaymentInfo()).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue["reader_model"]).isEqualTo(readerModel)
    }

    @Test
    fun `given no connected reader, when creating payment intent, then reader model is not set`() = runTest {
        val captor = argumentCaptor<Map<String, String>>()
        whenever(terminal.getConnectedReader()).thenReturn(null)

        action.createPaymentIntent(createPaymentInfo()).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue["reader_model"]).isNull()
    }

    @Test
    fun `when creating payment intent, then payment type set`() = runTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo()).toList()
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.PAYMENT_TYPE.key]).isEqualTo(MetaDataKeys.PaymentTypes.SINGLE.key)
    }

    @Test
    fun `when creating payment intent, then dollar amount converted to cents`() = runTest {
        whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }
        val amount = BigDecimal(1)

        action.createPaymentIntent(createPaymentInfo(amount = amount)).toList()

        verify(intentParametersBuilder).setAmount(100)
    }

    @Test
    fun `given payment info with order key, when creating payment intent, then order key is set`() {
        runTest {
            whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
                (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
            }
            val captor = argumentCaptor<Map<String, String>>()
            val orderKey = "order_key"

            action.createPaymentIntent(createPaymentInfo(orderKey = orderKey)).toList()
            verify(intentParametersBuilder).setMetadata(captor.capture())

            assertThat(captor.firstValue[MetaDataKeys.ORDER_KEY.key]).isEqualTo(orderKey)
        }
    }

    @Test
    fun `given payment info with order key is empty, when creating payment intent, then order key is not set`() {
        runTest {
            whenever(terminal.createPaymentIntent(any(), any())).thenAnswer {
                (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
            }
            val captor = argumentCaptor<Map<String, String>>()
            val orderKey = ""

            action.createPaymentIntent(createPaymentInfo(orderKey = orderKey)).toList()
            verify(intentParametersBuilder).setMetadata(captor.capture())

            assertThat(captor.firstValue[MetaDataKeys.ORDER_KEY.key]).isNull()
        }
    }

    @Test
    fun `given store in Canada, when creating payment intent, then payment method set`() = runTest {
        val expectedPaymentMethod = listOf(
            PaymentMethodType.CARD_PRESENT,
            PaymentMethodType.INTERAC_PRESENT
        )
        whenever(cardReaderConfigFactory.getCardReaderConfigFor("CA")).thenReturn(
            CardReaderConfigForCanada
        )

        action.createPaymentIntent(createPaymentInfo(countryCode = "CA")).toList()

        verify(paymentIntentParametersFactory).createBuilder(expectedPaymentMethod)
    }

    @Test
    fun `given store in US, when creating payment intent, then payment method set`() = runTest {
        val expectedPaymentMethod = listOf(
            PaymentMethodType.CARD_PRESENT
        )
        whenever(cardReaderConfigFactory.getCardReaderConfigFor("US")).thenReturn(
            CardReaderConfigForUSA
        )

        action.createPaymentIntent(createPaymentInfo(countryCode = "US")).toList()

        verify(paymentIntentParametersFactory).createBuilder(expectedPaymentMethod)
    }

    private fun createPaymentInfo(
        paymentDescription: String = "",
        orderId: Long = 1L,
        amount: BigDecimal = BigDecimal(0),
        currency: String = "USD",
        customerEmail: String? = "",
        wcpayCanSendReceipt: Boolean = false,
        customerName: String? = "",
        storeName: String? = "",
        siteUrl: String? = "",
        orderKey: String? = null,
        countryCode: String? = "US",
        statementDescriptor: String? = null,
    ): PaymentInfo =
        PaymentInfo(
            paymentDescription = paymentDescription,
            orderId = orderId,
            amount = amount,
            currency = currency,
            customerEmail = customerEmail,
            isPluginCanSendReceipt = wcpayCanSendReceipt,
            customerName = customerName,
            storeName = storeName,
            siteUrl = siteUrl,
            orderKey = orderKey,
            countryCode = countryCode,
            statementDescriptor = statementDescriptor
        )
}
