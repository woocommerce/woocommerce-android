package com.woocommerce.android.cardreader.receipts

import com.woocommerce.android.cardreader.receipts.PaymentCardBrand.VISA
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReceiptCreatorTest {
    private lateinit var creator: ReceiptCreator

    @Before
    fun setUp() {
        creator = ReceiptCreator()
    }

    @Test
    fun `generated html receipt is equal to expected example receipt`() {
        val expectedResult = ClassLoader.getSystemResource("example_receipt_html.html").readText()
        val receiptData = ReceiptData(
            staticTexts = ReceiptStaticTexts(
                applicationName = "Application name",
                receiptFromFormat = "Receipt from %s",
                receiptTitle = "Receipt",
                amountPaidSectionTitle = "Amount paid",
                datePaidSectionTitle = "Date paid",
                paymentMethodSectionTitle = "Payment method",
                summarySectionTitle = "Summary",
                aid = "AID"
            ),
            purchasedProducts = listOf(ReceiptLineItem(title = "T-shirt", quantity = 3, amount = 30)),
            amount = 30,
            currency = "USD",
            receiptDate = "19.5.2021",
            storeName = "The Best Woo store",
            applicationPreferredName = "WooCommerce for Android",
            dedicatedFileName = "abcdef",
            cardInfo = CardInfo("1234", VISA)
        )

        val result = creator.createHtmlReceipt(receiptData)

        assertThat(removeWhiteSpaces(result)).isEqualTo(removeWhiteSpaces(expectedResult))
    }

    private fun removeWhiteSpaces(input: String): String = input.replace("\\s".toRegex(), "")
}
