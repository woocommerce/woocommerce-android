package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.payments.MetaDataKeys
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@Suppress("DoNotMockDataClass")
@ExperimentalCoroutinesApi
internal class CreatePaymentActionTest : CardReaderBaseUnitTest() {
    private lateinit var action: CreatePaymentAction
    private val paymentIntentParametersFactory = mock<PaymentIntentParametersFactory>()
    private val terminal: TerminalWrapper = mock()
    private val intentParametersBuilder = mock<PaymentIntentParameters.Builder>()
    private val cardReaderConfigFactory: CardReaderConfigFactory = mock()
    private val paymentUtils: PaymentUtils = mock()

    @Before
    fun setUp() = testBlocking {
        action = CreatePaymentAction(
            paymentIntentParametersFactory,
            terminal,
            mock(),
            cardReaderConfigFactory,
            paymentUtils,
        )
        whenever(paymentIntentParametersFactory.createBuilder(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setAmount(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setCurrency(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setDescription(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setMetadata(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setReceiptEmail(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setApplicationFeeAmount(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.setStatementDescriptor(any())).thenReturn(intentParametersBuilder)
        whenever(intentParametersBuilder.build()).thenReturn(mock())
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(
            CardReaderConfigForUSA
        )
        whenever(terminal.createPaymentIntent(any())).thenReturn(mock())
    }

    @Test
    fun `when creating paymentIntent succeeds, then Success is emitted`() = testBlocking {
        whenever(terminal.createPaymentIntent(any())).thenReturn(mock())

        val result = action.createPaymentIntent(createPaymentInfo())

        assertThat(result).isExactlyInstanceOf(CreatePaymentStatus.Success::class.java)
    }

    @Test
    fun `when creating paymentIntent fails, then Failure is emitted`() = testBlocking {
        whenever(terminal.createPaymentIntent(any())).thenAnswer { throw mock<TerminalException>() }

        val result = action.createPaymentIntent(createPaymentInfo())

        assertThat(result).isExactlyInstanceOf(CreatePaymentStatus.Failure::class.java)
    }

    @Test
    fun `when creating paymentIntent succeeds, then updated paymentIntent is returned`() = testBlocking {
        val updatedPaymentIntent = mock<PaymentIntent>()
        whenever(terminal.createPaymentIntent(any())).thenReturn(updatedPaymentIntent)

        val result = action.createPaymentIntent(createPaymentInfo())

        assertThat((result as CreatePaymentStatus.Success).paymentIntent).isEqualTo(updatedPaymentIntent)
    }

    @Test
    fun `given wcpay can send emails, when customer email not empty, then PaymentIntent setReceiptEmail not invoked`() =
        testBlocking {
            val expectedEmail = "test@test.cz"

            action.createPaymentIntent(
                createPaymentInfo(
                    customerEmail = expectedEmail,
                    wcpayCanSendReceipt = true
                )
            )

            verify(intentParametersBuilder, never()).setReceiptEmail(any())
        }

    @Test
    fun `given wcpay can not send emails, when customer email not empty, then PaymentIntent setReceiptEmail invoked`() =
        testBlocking {
            val expectedEmail = "test@test.cz"

            action.createPaymentIntent(
                createPaymentInfo(
                    customerEmail = expectedEmail,
                    wcpayCanSendReceipt = false
                )
            )

            verify(intentParametersBuilder).setReceiptEmail(expectedEmail)
        }

    @Test
    fun `when customer email is null, then PaymentIntent setReceiptEmail not invoked`() = testBlocking {
        action.createPaymentIntent(createPaymentInfo(customerEmail = null))

        verify(intentParametersBuilder, never()).setReceiptEmail(any())
    }

    @Test
    fun `when customer email is empty, then PaymentIntent setReceiptEmail not invoked`() = testBlocking {
        action.createPaymentIntent(createPaymentInfo())

        verify(intentParametersBuilder, never()).setReceiptEmail(any())
    }

    @Test
    fun `when statement descriptor not empty, then PaymentIntent setStatementDescriptor invoked`() = testBlocking {
        val expected = "Site abcd"

        action.createPaymentIntent(createPaymentInfo(statementDescriptor = expected))

        verify(intentParametersBuilder).setStatementDescriptor(expected)
    }

    @Test
    fun `when statement descriptor empty, then PaymentIntent setStatementDescriptor NOT invoked`() = testBlocking {
        val expected = ""

        action.createPaymentIntent(createPaymentInfo(statementDescriptor = expected))

        verify(intentParametersBuilder, never()).setStatementDescriptor(any())
    }

    @Test
    fun `when statement descriptor null, then PaymentIntent setStatementDescriptor NOT invoked`() = testBlocking {
        val expected: String? = null

        action.createPaymentIntent(createPaymentInfo(statementDescriptor = expected))

        verify(intentParametersBuilder, never()).setStatementDescriptor(any())
    }

    @Test
    fun `when creating payment intent, then payment description set`() = testBlocking {
        val expectedDescription = "test description"

        action.createPaymentIntent(createPaymentInfo(paymentDescription = expectedDescription))

        verify(intentParametersBuilder).setDescription(expectedDescription)
    }

    @Test
    fun `when statement fee null, then PaymentIntent setApplicationFeeAmount NOT invoked`() = testBlocking {
        action.createPaymentIntent(createPaymentInfo())

        verify(intentParametersBuilder, never()).setApplicationFeeAmount(any())
    }

    @Test
    fun `when statement fee is not null, then PaymentIntent setApplicationFeeAmount invoked`() = testBlocking {
        val expected = 100L

        action.createPaymentIntent(createPaymentInfo(feeAmount = expected))

        verify(intentParametersBuilder).setApplicationFeeAmount(expected)
    }

    @Test
    fun `when creating payment intent, then store name set`() = testBlocking {
        val expected = "dummy store name"
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(storeName = expected))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.STORE.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then customer name set`() = testBlocking {
        val expected = "dummy customer name"
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(customerName = expected))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.CUSTOMER_NAME.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then customer email set`() = testBlocking {
        val expected = "dummy customer email"
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(customerEmail = expected))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.CUSTOMER_EMAIL.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then site url set`() = testBlocking {
        val expected = "dummy site url"
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(siteUrl = expected))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.SITE_URL.key]).isEqualTo(expected)
    }

    @Test
    fun `when creating payment intent, then order id set`() = testBlocking {
        val expected = 99L
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(orderId = expected))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.ORDER_ID.key]).isEqualTo(expected.toString())
    }

    @Test
    fun `when creating payment intent, then reader id set`() = testBlocking {
        val readerId = "SM12345678"
        val captor = argumentCaptor<Map<String, String>>()
        val reader = mock<CardReader> { on { id }.thenReturn(readerId) }
        whenever(terminal.getConnectedReader()).thenReturn(reader)

        action.createPaymentIntent(createPaymentInfo())
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.READER_ID.key]).isEqualTo(readerId)
    }

    @Test
    fun `given connected reader, when creating payment intent, then reader model set`() = testBlocking {
        val readerModel = "STRIPE_M2"
        val captor = argumentCaptor<Map<String, String>>()
        val reader = mock<CardReader> { on { type }.thenReturn(readerModel) }
        whenever(terminal.getConnectedReader()).thenReturn(reader)

        action.createPaymentIntent(createPaymentInfo())
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue["reader_model"]).isEqualTo(readerModel)
    }

    @Test
    fun `given no connected reader, when creating payment intent, then reader model is not set`() = testBlocking {
        val captor = argumentCaptor<Map<String, String>>()
        whenever(terminal.getConnectedReader()).thenReturn(null)

        action.createPaymentIntent(createPaymentInfo())
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue["reader_model"]).isNull()
    }

    @Test
    fun `when creating payment intent, then payment type set`() = testBlocking {
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo())
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.PAYMENT_TYPE.key]).isEqualTo(MetaDataKeys.PaymentTypes.SINGLE.key)
    }

    @Test
    fun `when creating payment intent, then dollar amount converted to cents`() = testBlocking {
        val amount = BigDecimal(1)
        whenever(paymentUtils.convertToSmallestCurrencyUnit(eq(amount), eq("USD"))).thenReturn(100L)

        action.createPaymentIntent(createPaymentInfo(amount = amount))

        verify(intentParametersBuilder).setAmount(100)
    }

    @Test
    fun `given payment info with order key, when creating payment intent, then order key is set`() = testBlocking {
        val captor = argumentCaptor<Map<String, String>>()
        val orderKey = "order_key"

        action.createPaymentIntent(createPaymentInfo(orderKey = orderKey))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.ORDER_KEY.key]).isEqualTo(orderKey)
    }

    @Test
    fun `given payment info with order key is empty, when creating payment intent, then order key is not set`() =
        testBlocking {
            val captor = argumentCaptor<Map<String, String>>()
            val orderKey = ""

            action.createPaymentIntent(createPaymentInfo(orderKey = orderKey))
            verify(intentParametersBuilder).setMetadata(captor.capture())

            assertThat(captor.firstValue[MetaDataKeys.ORDER_KEY.key]).isNull()
        }

    @Test
    fun `given store in Canada, when creating payment intent, then payment method set`() = testBlocking {
        val expectedPaymentMethod = listOf(
            PaymentMethodType.CARD_PRESENT,
            PaymentMethodType.INTERAC_PRESENT
        )
        whenever(cardReaderConfigFactory.getCardReaderConfigFor("CA")).thenReturn(
            CardReaderConfigForCanada
        )

        action.createPaymentIntent(createPaymentInfo(countryCode = "CA"))

        verify(paymentIntentParametersFactory).createBuilder(expectedPaymentMethod)
    }

    @Test
    fun `given store in US, when creating payment intent, then payment method set`() = testBlocking {
        val expectedPaymentMethod = listOf(
            PaymentMethodType.CARD_PRESENT
        )
        whenever(cardReaderConfigFactory.getCardReaderConfigFor("US")).thenReturn(
            CardReaderConfigForUSA
        )

        action.createPaymentIntent(createPaymentInfo(countryCode = "US"))

        verify(paymentIntentParametersFactory).createBuilder(expectedPaymentMethod)
    }

    @Test
    fun `when creating payment intent, then platform set to android`() = testBlocking {
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo())
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue["platform"]).isEqualTo("android")
    }

    @Test
    fun `given payment info with pos channel, when creating payment intent, then channel is set`() = testBlocking {
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(channel = PaymentInfo.PaymentChannel.Pos))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.CHANNEL.key]).isEqualTo("mobile_pos")
    }

    @Test
    fun `given payment info with store manager channel, when creating payment intent, then channel is set`() =
        testBlocking {
            val captor = argumentCaptor<Map<String, String>>()

            action.createPaymentIntent(createPaymentInfo(channel = PaymentInfo.PaymentChannel.StoreManager))
            verify(intentParametersBuilder).setMetadata(captor.capture())

            assertThat(captor.firstValue[MetaDataKeys.CHANNEL.key]).isEqualTo("mobile_store_management")
        }

    @Test
    fun `given payment info with no channel, when creating payment intent, then channel is not set`() = testBlocking {
        val captor = argumentCaptor<Map<String, String>>()

        action.createPaymentIntent(createPaymentInfo(channel = null))
        verify(intentParametersBuilder).setMetadata(captor.capture())

        assertThat(captor.firstValue[MetaDataKeys.CHANNEL.key]).isNull()
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
        feeAmount: Long? = null,
        channel: PaymentInfo.PaymentChannel? = null,
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
            statementDescriptor = StatementDescriptor(statementDescriptor),
            feeAmount = feeAmount,
            channel = channel,
        )
}
