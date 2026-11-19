package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.LabeledText
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Row
import javax.inject.Inject

/**
 * Text is measured with [String.length], which matches printed columns for monospaced Latin text only.
 */
class WooPosThermalReceiptRenderer @Inject constructor() {
    fun render(receipt: WooPosThermalReceipt): String =
        listOf(
            header(receipt),
            payment(receipt),
            receipt.lineItems.flatMap(::twoColumns),
            receipt.totals.flatMap(::twoColumns),
            labeled(receipt.orderNote),
            wrap(receipt.refundPolicy),
        )
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n$SEPARATOR\n", postfix = "\n") { it.joinToString("\n") }

    private fun header(receipt: WooPosThermalReceipt): List<String> =
        listOf(receipt.storeName, receipt.storeAddress, receipt.storePhone, receipt.storeEmail)
            .flatMap(::wrap)
            .map { it.padStart((LINE_WIDTH + it.length) / 2) }

    private fun payment(receipt: WooPosThermalReceipt): List<String> =
        labeled(receipt.datePaid) + wrap(receipt.orderNumber) + labeled(receipt.paymentMethod)

    private fun labeled(labeledText: LabeledText?): List<String> =
        wrap(labeledText?.text).let { if (it.isEmpty()) it else wrap(labeledText?.label) + it }

    private fun twoColumns(row: Row): List<String> {
        val labelLines = wrap(row.label)
        val amountLines = wrap(row.amount)
        val lastLine = labelLines.lastOrNull().orEmpty()
        val amount = amountLines.singleOrNull()
        return if (amount != null && lastLine.length + 1 + amount.length <= LINE_WIDTH) {
            labelLines.dropLast(1) + (lastLine + amount.padStart(LINE_WIDTH - lastLine.length))
        } else {
            labelLines + amountLines.map { it.padStart(LINE_WIDTH) }
        }
    }

    private fun wrap(text: String?): List<String> = text.orEmpty().lines().flatMap(::wrapLine)

    private fun wrapLine(line: String): List<String> =
        line.split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .flatMap(::hardSplit)
            .fold(emptyList<String>()) { lines, word ->
                val lastLine = lines.lastOrNull()
                if (lastLine != null && lastLine.length + 1 + word.length <= LINE_WIDTH) {
                    lines.dropLast(1) + "$lastLine $word"
                } else {
                    lines + word
                }
            }

    private fun hardSplit(word: String): List<String> =
        generateSequence(word) { it.drop(splitIndex(it)).ifEmpty { null } }
            .map { it.take(splitIndex(it)) }
            .toList()

    private fun splitIndex(word: String): Int =
        if (word.length > LINE_WIDTH && word[LINE_WIDTH - 1].isHighSurrogate()) LINE_WIDTH - 1 else LINE_WIDTH

    private companion object {
        const val LINE_WIDTH = 48
        val SEPARATOR = "-".repeat(LINE_WIDTH)
        val WHITESPACE = Regex("\\s+")
    }
}
