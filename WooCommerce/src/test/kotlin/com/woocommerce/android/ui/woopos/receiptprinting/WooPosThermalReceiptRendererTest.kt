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
    fun `given a store name longer than 48 characters, when rendering, then each wrapped line is centered`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(
            storeName = "The Everyday Pay Flagship Store and Coffee Roasters of Springfield",
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).startsWith(
            """
               The Everyday Pay Flagship Store and Coffee
                        Roasters of Springfield
            ------------------------------------------------
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given an amount longer than 48 characters, when rendering, then it wraps below its label right aligned`() {
        // GIVEN
        val totals = MINIMAL_RECEIPT.totals.copy(subtotal = "9".repeat(60))

        // WHEN
        val text = sut.render(MINIMAL_RECEIPT.copy(totals = totals), LABELS)

        // THEN
        assertThat(text).endsWith(
            """
            Subtotal
            999999999999999999999999999999999999999999999999
                                                999999999999
            Amount Paid                               $12.00
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given an amount with a line break, when rendering, then each amount line is right aligned`() {
        // GIVEN
        val totals = MINIMAL_RECEIPT.totals.copy(subtotal = "$10.00\n$2.00")

        // WHEN
        val text = sut.render(MINIMAL_RECEIPT.copy(totals = totals), LABELS)

        // THEN
        assertThat(text).endsWith(
            """
            Subtotal
                                                      $10.00
                                                       $2.00
            Amount Paid                               $12.00
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `given a split that would fall inside an emoji, when rendering, then the emoji moves to the next line`() {
        // GIVEN
        val emoji = "\uD83D\uDE00"
        val receipt = MINIMAL_RECEIPT.copy(orderNote = "a".repeat(47) + emoji)

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).endsWith("Notes\n${"a".repeat(47)}\n$emoji\n")
    }

    @Test
    fun `given a label that leaves one space before the amount, when rendering, then they share the row`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(
            lineItems = listOf(
                LineItem(title = "a".repeat(38), attributes = "", quantity = "1", amount = "$12.00"),
                LineItem(title = "b".repeat(39), attributes = "", quantity = "1", amount = "$12.00"),
            ),
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).contains(
            "${"a".repeat(38)} x1 $12.00\n" +
                "${"b".repeat(39)} x1\n" +
                "${" ".repeat(42)}$12.00\n"
        )
    }

    @Test
    fun `given a receipt with only blank text, when rendering, then only the totals labels are printed`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(
            storeName = " ",
            datePaid = "",
            orderNumber = "",
            lineItems = emptyList(),
            totals = MINIMAL_RECEIPT.totals.copy(subtotal = "", amountPaid = ""),
            orderNote = "\n",
            card = Card(brand = "", last4 = " "),
            returnsPolicy = "\t",
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).isEqualTo("Subtotal\nAmount Paid\n")
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
            orderNumber = "Order #1042",
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
