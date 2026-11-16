package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Labels
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Row
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
        val text = sut.render(FULL_RECEIPT.copy(paymentMethod = null), LABELS)

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
        val receipts = listOf("\r\n", "\n", "\r").map { lineBreak ->
            MINIMAL_RECEIPT.copy(storeAddress = "12345 Example Street S${lineBreak}Springfield Heights, CA 90000")
        }

        // WHEN
        val texts = receipts.map { sut.render(it, LABELS) }

        // THEN
        assertThat(texts.distinct()).hasSize(1)
    }

    @Test
    fun `given labels too long for one row, when rendering, then labels wrap and amounts stay right aligned`() {
        // GIVEN
        val receipt = MINIMAL_RECEIPT.copy(
            lineItems = listOf(
                Row(label = "Extra Large Premium Organic Cotton Hooded Sweatshirt x1", amount = "$89.00"),
                Row(
                    label = "Hand-Thrown Stoneware Dinner Plate Set of Six. Color Midnight Blue, Finish Matte Glaze x2",
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
        val totals = listOf(
            Row(label = "Subtotal", amount = "9".repeat(60)),
            Row(label = "Amount Paid", amount = "$12.00"),
        )

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
        val totals = listOf(
            Row(label = "Subtotal", amount = "$10.00\n$2.00"),
            Row(label = "Amount Paid", amount = "$12.00"),
        )

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
                Row(label = "a".repeat(41), amount = "$12.00"),
                Row(label = "b".repeat(42), amount = "$12.00"),
            ),
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).contains(
            "${"a".repeat(41)} $12.00\n" +
                "${"b".repeat(42)}\n" +
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
            paymentMethod = " ",
            totals = listOf(Row(label = "Subtotal", amount = ""), Row(label = "Amount Paid", amount = "")),
            orderNote = "\n",
            refundPolicy = "\t",
        )

        // WHEN
        val text = sut.render(receipt, LABELS)

        // THEN
        assertThat(text).isEqualTo("Subtotal\nAmount Paid\n")
    }

    @Test
    fun `given no store details, items, note or refund policy, when rendering, then their sections are skipped`() {
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
            paymentMethod = wide,
            lineItems = listOf(Row(label = wide, amount = wide)),
            totals = listOf(Row(label = wide, amount = wide)),
            orderNote = wide,
            refundPolicy = wide,
        )
        val labels = Labels(datePaid = wide, paymentMethod = wide, notes = wide)

        // WHEN
        val text = sut.render(receipt, labels)

        // THEN
        assertThat(text.lines().maxOf { it.length }).isEqualTo(48)
    }

    private companion object {
        val LABELS = Labels(datePaid = "Date Paid", paymentMethod = "Payment Method", notes = "Notes")

        val MINIMAL_RECEIPT = WooPosThermalReceipt(
            storeName = "Everyday Pay",
            storeAddress = null,
            storePhone = null,
            storeEmail = null,
            datePaid = "Nov 12, 2026, 2:30 PM",
            orderNumber = "Order #1042",
            paymentMethod = null,
            lineItems = listOf(Row(label = "Coffee Mug x1", amount = "$12.00")),
            totals = listOf(Row(label = "Subtotal", amount = "$12.00"), Row(label = "Amount Paid", amount = "$12.00")),
            orderNote = null,
            refundPolicy = null,
        )

        val FULL_RECEIPT = MINIMAL_RECEIPT.copy(
            storeAddress = "12345 Example Street S",
            storePhone = "05555 465 5556",
            storeEmail = "shop@everyday-pay.example",
            paymentMethod = "Visa - 4242",
            lineItems = listOf(
                Row(label = "T-Shirt. Color Blue, Size M x2", amount = "$50.00"),
                Row(label = "Coffee Mug x1", amount = "$12.00"),
            ),
            totals = listOf(
                Row(label = "Subtotal", amount = "$62.00"),
                Row(label = "Discount (SAVE10, VIP)", amount = "-$6.20"),
                Row(label = "Fees", amount = "$1.00"),
                Row(label = "Shipping", amount = "$5.00"),
                Row(label = "Taxes", amount = "$4.94"),
                Row(label = "Amount Paid", amount = "$66.74"),
            ),
            orderNote = "Thanks for shopping with us!",
            refundPolicy = "Returns accepted within 30 days with the original receipt.",
        )
    }
}
