package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Card
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Discount
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Labels
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.LineItem
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Totals
import javax.inject.Inject

/**
 * Text is measured with [String.length], which matches printed columns for monospaced Latin text only.
 */
class WooPosThermalReceiptRenderer @Inject constructor() {
    fun render(receipt: WooPosThermalReceipt, labels: Labels): String =
        listOf(
            header(receipt),
            payment(receipt, labels),
            receipt.lineItems.flatMap(::lineItem),
            totals(receipt.totals, labels),
            note(receipt.orderNote, labels),
            wrap(receipt.returnsPolicy),
        )
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n$SEPARATOR\n", postfix = "\n") { it.joinToString("\n") }

    private fun header(receipt: WooPosThermalReceipt): List<String> =
        listOf(receipt.storeName, receipt.storeAddress, receipt.storePhone, receipt.storeEmail)
            .flatMap(::wrap)
            .map { it.padStart((LINE_WIDTH + it.length) / 2) }

    private fun payment(receipt: WooPosThermalReceipt, labels: Labels): List<String> {
        val order = listOf(labels.datePaid, receipt.datePaid, labels.orderNumber.format(receipt.orderNumber))
        val card = receipt.card?.let { listOf(labels.paymentMethod, cardDescription(it)) }.orEmpty()
        return (order + card).flatMap(::wrap)
    }

    private fun cardDescription(card: Card): String =
        listOf(card.brand, card.last4).filter { it.isNotBlank() }.joinToString(" - ")

    private fun lineItem(item: LineItem): List<String> {
        val title = listOf(item.title, item.attributes).filter { it.isNotBlank() }.joinToString(". ")
        return twoColumns("$title x${item.quantity}", item.amount)
    }

    private fun totals(totals: Totals, labels: Labels): List<String> =
        listOfNotNull(
            labels.subtotal to totals.subtotal,
            totals.discount?.let { discountLabel(labels.discount, it) to it.amount },
            totals.fees?.let { labels.fees to it },
            totals.shipping?.let { labels.shipping to it },
            totals.taxes?.let { labels.taxes to it },
            labels.amountPaid to totals.amountPaid,
        ).flatMap { (label, amount) -> twoColumns(label, amount) }

    private fun discountLabel(label: String, discount: Discount): String =
        if (discount.couponCodes.isEmpty()) label else "$label (${discount.couponCodes.joinToString()})"

    private fun note(orderNote: String, labels: Labels): List<String> =
        wrap(orderNote).let { if (it.isEmpty()) it else wrap(labels.notes) + it }

    private fun twoColumns(left: String, right: String): List<String> {
        val leftLines = wrap(left)
        val lastLine = leftLines.lastOrNull().orEmpty()
        return if (lastLine.length + 1 + right.length <= LINE_WIDTH) {
            leftLines.dropLast(1) + (lastLine + right.padStart(LINE_WIDTH - lastLine.length))
        } else {
            leftLines + wrap(right).map { it.padStart(LINE_WIDTH) }
        }
    }

    private fun wrap(text: String): List<String> = text.lines().flatMap(::wrapLine)

    private fun wrapLine(line: String): List<String> =
        line.split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .flatMap { it.chunked(LINE_WIDTH) }
            .fold(emptyList<String>()) { lines, word ->
                val lastLine = lines.lastOrNull()
                if (lastLine != null && lastLine.length + 1 + word.length <= LINE_WIDTH) {
                    lines.dropLast(1) + "$lastLine $word"
                } else {
                    lines + word
                }
            }

    private companion object {
        const val LINE_WIDTH = 48
        val SEPARATOR = "-".repeat(LINE_WIDTH)
        val WHITESPACE = Regex("\\s+")
    }
}
