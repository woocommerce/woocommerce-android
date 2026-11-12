package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Card
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Discount
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Labels
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.LineItem
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Totals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosThermalReceiptRendererTest {
    private val sut = WooPosThermalReceiptRenderer()

    @Test
    fun `given a card payment, when rendering, then the receipt has every section and the payment method`() {
        // WHEN
        val text = sut.render(FULL_RECEIPT, LABELS)

        // THEN
        assertThat(text).isEqualTo(
            """
                              Everyday Pay
                         12345 Example Street S
                             05555 465 5556
                       shop@everyday-pay.example
            ------------------------------------------------
            Date Paid
            Nov 12, 2026, 2:30 PM
            Order #1042
            Payment Method
            Visa - 4242
            ------------------------------------------------
            T-Shirt. Color Blue, Size M x2            $50.00
            Coffee Mug x1                             $12.00
            ------------------------------------------------
            Subtotal                                  $62.00
            Discount (SAVE10, VIP)                    -$6.20
            Fees                                       $1.00
            Shipping                                   $5.00
            Taxes                                      $4.94
            Amount Paid                               $66.74
            ------------------------------------------------
            Notes
            Thanks for shopping with us!
            ------------------------------------------------
            Returns accepted within 30 days with the
            original receipt.
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a cash payment, when rendering, then the receipt has no payment method`() {
        // WHEN
        val text = sut.render(FULL_RECEIPT.copy(card = null), LABELS)

        // THEN
        assertThat(text).isEqualTo(
            """
                              Everyday Pay
                         12345 Example Street S
                             05555 465 5556
                       shop@everyday-pay.example
            ------------------------------------------------
            Date Paid
            Nov 12, 2026, 2:30 PM
            Order #1042
            ------------------------------------------------
            T-Shirt. Color Blue, Size M x2            $50.00
            Coffee Mug x1                             $12.00
            ------------------------------------------------
            Subtotal                                  $62.00
            Discount (SAVE10, VIP)                    -$6.20
            Fees                                       $1.00
            Shipping                                   $5.00
            Taxes                                      $4.94
            Amount Paid                               $66.74
            ------------------------------------------------
            Notes
            Thanks for shopping with us!
            ------------------------------------------------
            Returns accepted within 30 days with the
            original receipt.
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a store address with CRLF line breaks, when rendering, then each address line is centered`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(
            storeAddress = "12345 Example Street S\r\nSpringfield Heights, CA 90000",
            storePhone = "05555 465 5556",
            storeEmail = "shop@everyday-pay.example",
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).isEqualTo(
            """
                              Everyday Pay
                         12345 Example Street S
                     Springfield Heights, CA 90000
                             05555 465 5556
                       shop@everyday-pay.example
            ------------------------------------------------
            Date Paid
            Nov 12, 2026, 2:30 PM
            Order #1042
            ------------------------------------------------
            Coffee Mug x1                             $12.00
            ------------------------------------------------
            Subtotal                                  $12.00
            Amount Paid                               $12.00
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a store address with LF or CR line breaks, when rendering, then it prints like the CRLF one`() {
        // GIVEN
        val crlf = MINIMAL_RECEIPT.copy(storeAddress = "12345 Example Street S\r\nSpringfield Heights, CA 90000")

        // WHEN
        val texts = listOf("\n", "\r").map { lineBreak ->
            sut.render(crlf.copy(storeAddress = crlf.storeAddress.replace("\r\n", lineBreak)), LABELS)
        }

        // THEN
        assertThat(texts).containsOnly(sut.render(crlf, LABELS))
    }

    @Test
    fun `given item titles too long for one row, when rendering, then titles wrap and amounts stay right aligned`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(
            lineItems = listOf(
                LineItem(
                    title = "Extra Large Premium Organic Cotton Hooded Sweatshirt",
                    attributes = "",
                    quantity = "1",
                    amount = "$89.00",
                ),
                LineItem(
                    title = "Hand-Thrown Stoneware Dinner Plate Set of Six",
                    attributes = "Color Midnight Blue, Finish Matte Glaze",
                    quantity = "2",
                    amount = "$1,249.00",
                ),
            ),
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).isEqualTo(
            """
                              Everyday Pay
            ------------------------------------------------
            Date Paid
            Nov 12, 2026, 2:30 PM
            Order #1042
            ------------------------------------------------
            Extra Large Premium Organic Cotton Hooded
            Sweatshirt x1                             $89.00
            Hand-Thrown Stoneware Dinner Plate Set of Six.
            Color Midnight Blue, Finish Matte Glaze x2
                                                   $1,249.00
            ------------------------------------------------
            Subtotal                                  $12.00
            Amount Paid                               $12.00
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a word longer than 48 characters, when rendering, then the word is split across lines`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(orderNote = "Code ${"0123456789".repeat(10)} thanks")

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).endsWith(
            """
            Notes
            Code
            012345678901234567890123456789012345678901234567
            890123456789012345678901234567890123456789012345
            6789 thanks
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given no store details, items, note or returns policy, when rendering, then their sections are skipped`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(storeName = "", lineItems = emptyList())

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).isEqualTo(
            """
            Date Paid
            Nov 12, 2026, 2:30 PM
            Order #1042
            ------------------------------------------------
            Subtotal                                  $12.00
            Amount Paid                               $12.00
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a card without a brand, when rendering, then the payment method is the last 4 digits`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(card = Card(brand = "", last4 = "4242"))

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).contains(
            """
            Payment Method
            4242
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a discount without coupon codes, when rendering, then the discount label has no codes`() {
        // GIVEN
        val totals = MINIMAL_RECEIPT.totals.copy(discount = Discount(couponCodes = emptyList(), amount = "-$6.20"))

        // WHEN
        val text = sut.render(MINIMAL_RECEIPT.copy(totals = totals), LABELS)

        // THEN
        assertThat(text.lines()).contains(
            """
            Discount                                  -$6.20
            """.trimIndent()
        )
    }

    @Test
    fun `given every field is wider than the paper, when rendering, then no line is longer than 48 characters`() {
        // GIVEN
        val wide = "Wider than the paper ${"9".repeat(60)} by any measure, whichever way it is wrapped"
        val receipt = WooPosThermalReceipt(
            storeName = wide,
            storeAddress = "$wide\r\n$wide",
            storePhone = wide,
            storeEmail = wide,
            datePaid = wide,
            orderNumber = wide,
            lineItems = listOf(LineItem(title = wide, attributes = wide, quantity = wide, amount = wide)),
            totals = Totals(
                subtotal = wide,
                discount = Discount(couponCodes = listOf(wide, wide), amount = wide),
                fees = wide,
                shipping = wide,
                taxes = wide,
                amountPaid = wide,
            ),
            orderNote = wide,
            card = Card(brand = wide, last4 = wide),
            returnsPolicy = wide,
        )
        val labels = Labels(
            datePaid = wide,
            orderNumber = "$wide %1\$s",
            paymentMethod = wide,
            subtotal = wide,
            discount = wide,
            fees = wide,
            shipping = wide,
            taxes = wide,
            amountPaid = wide,
            notes = wide,
        )

        // WHEN
        val text = sut.render(receipt, labels)

        // THEN
        assertThat(text.lines().maxOf { it.length }).isEqualTo(48)
    }

    private companion object {
        val LABELS = Labels(
            datePaid = "Date Paid",
            orderNumber = "Order #%1\$s",
            paymentMethod = "Payment Method",
            subtotal = "Subtotal",
            discount = "Discount",
            fees = "Fees",
            shipping = "Shipping",
            taxes = "Taxes",
            amountPaid = "Amount Paid",
            notes = "Notes",
        )

        val MINIMAL_RECEIPT = WooPosThermalReceipt(
            storeName = "Everyday Pay",
            storeAddress = "",
            storePhone = "",
            storeEmail = "",
            datePaid = "Nov 12, 2026, 2:30 PM",
            orderNumber = "1042",
            lineItems = listOf(LineItem(title = "Coffee Mug", attributes = "", quantity = "1", amount = "$12.00")),
            totals = Totals(
                subtotal = "$12.00",
                discount = null,
                fees = null,
                shipping = null,
                taxes = null,
                amountPaid = "$12.00",
            ),
            orderNote = "",
            card = null,
            returnsPolicy = "",
        )

        val FULL_RECEIPT = MINIMAL_RECEIPT.copy(
            storeAddress = "12345 Example Street S",
            storePhone = "05555 465 5556",
            storeEmail = "shop@everyday-pay.example",
            lineItems = listOf(
                LineItem(title = "T-Shirt", attributes = "Color Blue, Size M", quantity = "2", amount = "$50.00"),
                LineItem(title = "Coffee Mug", attributes = "", quantity = "1", amount = "$12.00"),
            ),
            totals = Totals(
                subtotal = "$62.00",
                discount = Discount(couponCodes = listOf("SAVE10", "VIP"), amount = "-$6.20"),
                fees = "$1.00",
                shipping = "$5.00",
                taxes = "$4.94",
                amountPaid = "$66.74",
            ),
            orderNote = "Thanks for shopping with us!",
            card = Card(brand = "Visa", last4 = "4242"),
            returnsPolicy = "Returns accepted within 30 days with the original receipt.",
        )
    }
}
