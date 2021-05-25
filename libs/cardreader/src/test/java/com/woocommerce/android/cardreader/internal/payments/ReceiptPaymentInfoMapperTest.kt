package com.woocommerce.android.cardreader.internal.payments

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.model.external.CardPresentDetails
import com.stripe.stripeterminal.model.external.Charge
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.PaymentMethodDetails
import com.stripe.stripeterminal.model.external.ReceiptDetails
import com.woocommerce.android.cardreader.receipts.PaymentCardBrand
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReceiptPaymentInfoMapperTest {
    private lateinit var receiptPaymentInfoMapper: ReceiptPaymentInfoMapper
    private var mockedPaymentIntent: PaymentIntent = mock()
    private var mockedCharge: Charge = mock()
    private var mockedPaymentMethodDetails: PaymentMethodDetails = mock()
    private var mockedCardPresentDetails: CardPresentDetails = mock()
    private var mockedReceiptDetails: ReceiptDetails = mock()

    @Before
    fun setUp() {
        receiptPaymentInfoMapper = ReceiptPaymentInfoMapper()

        whenever(mockedPaymentIntent.getCharges()).thenReturn(listOf(mockedCharge))
        whenever(mockedCharge.paymentMethodDetails).thenReturn(mockedPaymentMethodDetails)
        whenever(mockedPaymentMethodDetails.cardPresentDetails).thenReturn(mockedCardPresentDetails)
        whenever(mockedCardPresentDetails.receiptDetails).thenReturn(mockedReceiptDetails)

        whenever(mockedCharge.amount).thenReturn(900)
        whenever(mockedCharge.created).thenReturn(901)
        whenever(mockedReceiptDetails.applicationPreferredName).thenReturn("a")
        whenever(mockedReceiptDetails.dedicatedFileName).thenReturn("b")
    }

    @Test
    fun `when all required data available, then ReceiptPaymentInfo returned`() {
        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result).isNotNull
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when application preferred name not available, then exception thrown`() {
        whenever(mockedReceiptDetails.applicationPreferredName).thenReturn(null)

        receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when dedicated file name not available, then exception thrown`() {
        whenever(mockedReceiptDetails.dedicatedFileName).thenReturn(null)

        receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when charge not available, then exception thrown`() {
        whenever(mockedPaymentIntent.getCharges()).thenReturn(listOf())

        receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when payment method details not available, then exception thrown`() {
        whenever(mockedCharge.paymentMethodDetails).thenReturn(null)

        receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when card present details not available, then exception thrown`() {
        whenever(mockedPaymentMethodDetails.cardPresentDetails).thenReturn(null)

        receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when card receipt details not available, then exception thrown`() {
        whenever(mockedCardPresentDetails.receiptDetails).thenReturn(null)

        receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)
    }

    @Test
    fun `when mapping total amount, then original amount converted from cents to dollars`() {
        whenever(mockedCharge.amount).thenReturn(149)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.chargedAmount).isEqualTo(1.49f)
    }

    @Test
    fun `when currency null, then empty string is used`() {
        whenever(mockedCharge.currency).thenReturn(null)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.currency).isEqualTo("")
    }

    @Test
    fun `when card brand is null, then Unknown PaymentCardBrand is used`() {
        whenever(mockedCardPresentDetails.brand).thenReturn(null)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.cardInfo.brand).isEqualTo(PaymentCardBrand.UNKNOWN)
    }

    @Test
    fun `when card brand is empty, then Unknown PaymentCardBrand is used`() {
        whenever(mockedCardPresentDetails.brand).thenReturn("")

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.cardInfo.brand).isEqualTo(PaymentCardBrand.UNKNOWN)
    }

    @Test
    fun `when card brand is visa, then VISA PaymentCardBrand is used`() {
        whenever(mockedCardPresentDetails.brand).thenReturn("visa")

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.cardInfo.brand).isEqualTo(PaymentCardBrand.VISA)
    }

    @Test
    fun `when card brand is mastercard, then MASTER_CARD PaymentCardBrand is used`() {
        whenever(mockedCardPresentDetails.brand).thenReturn("mastercard")

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.cardInfo.brand).isEqualTo(PaymentCardBrand.MASTER_CARD)
    }

    @Test
    fun `when mapping currency, then currency field is used`() {
        whenever(mockedCharge.currency).thenReturn(null)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.currency).isEqualTo("")
    }

    @Test
    fun `when mapping receiptDate, then charge created field is used`() {
        val expectedReceiptDate = 123L
        whenever(mockedCharge.created).thenReturn(expectedReceiptDate)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.receiptDate).isEqualTo(expectedReceiptDate)
    }

    @Test
    fun `when mapping last4CardDigits, then last4 field is used`() {
        val expectedLast4CardDigits = "1234"
        whenever(mockedCardPresentDetails.last4).thenReturn(expectedLast4CardDigits)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.cardInfo.last4CardDigits).isEqualTo(expectedLast4CardDigits)
    }

    @Test
    fun `when mapping application preferred name, then applicationPreferredName field is used`() {
        val expectedApplicationPreferredName = "app"
        whenever(mockedReceiptDetails.applicationPreferredName).thenReturn(expectedApplicationPreferredName)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.applicationPreferredName).isEqualTo(expectedApplicationPreferredName)
    }

    @Test
    fun `when mapping dedicated file name, then dedicatedFileName field is used`() {
        val expectedApplicationPreferredName = "app"
        whenever(mockedReceiptDetails.applicationPreferredName).thenReturn(expectedApplicationPreferredName)

        val result = receiptPaymentInfoMapper.mapPaymentIntentToPaymentInfo(mockedPaymentIntent)

        assertThat(result.applicationPreferredName).isEqualTo(expectedApplicationPreferredName)
    }
}
