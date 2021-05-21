package com.woocommerce.android.cardreader.receipts

import java.util.Locale
import javax.inject.Inject

/**
 * Most parts of this class were copied from woocommerce-ios codebase. Ideally don't make vital changes to the structure
 * as we want to keep the solution consistent across platforms. The more similar the code is the easier it is to copy
 * changes from iOS to Android and vice versa.
 */
class ReceiptCreator @Inject constructor() {
    // TODO cardreader ideally move receipt creation to the backend
    // todo cardreader add a date formatter so we can format receiptDate
    fun createHtmlReceipt(receiptData: ReceiptData): String {
        return """
            <html>
            <head>
                <style type="text/css">
                    html { font-family: "Helvetica Neue", sans-serif; font-size: ${FONT_SIZE}pt; }
                    header { margin-top: $MARGIN; }
                    h1 { font-size: ${TITLE_FONT_SIZE}pt; font-weight: 500; text-align: center; }
                    h3 { color: #707070; margin:0; }
                    table {
                        background-color:#F5F5F5;
                        width:100%;
                        color: #707070;
                        margin: ${MARGIN / 2}pt 0 0 0;
                        padding: ${MARGIN / 2}pt;
                    }
                    table td:last-child { width: 30%; text-align: right; }
                    table tr:last-child { color: #000000; font-weight: bold; }
                    footer {
                        font-size: ${FOOTER_FONT_SIZE}pt;
                        border-top: 1px solid #707070;
                        margin-top: ${MARGIN}pt;
                        padding-top: ${MARGIN}pt;
                    }
                    .card-icon {
                       width: ${ICON_WIDTH}pt;
                       height: ${ICON_HEIGHT}pt;
                       vertical-align: top;
                       background-repeat: no-repeat;
                       background-position-y: center;
                       display: inline-block;
                    }
                    p { line-height: ${LINE_HEIGHT}pt; margin: 0 0 ${MARGIN / 2} 0; }
                    ${buildIconCSS(receiptData.receiptPaymentInfo.cardInfo.brand)}
                </style>
            </head>
                <body>
                    <header>
                        <h1>${buildReceiptTitle(receiptData)}</h1>
                        <h3>${receiptData.staticTexts.amountPaidSectionTitle.toUpperCase(Locale.getDefault())}</h3>
                        <p>
                            ${"%.2f".format(receiptData.receiptPaymentInfo.chargedAmount)} ${
            receiptData.receiptPaymentInfo.currency.toUpperCase(
                Locale.getDefault()
            )
        }
                        </p>
                        <h3>${receiptData.staticTexts.datePaidSectionTitle.toUpperCase(Locale.getDefault())}</h3>
                        <p>
                            ${receiptData.receiptPaymentInfo.receiptDate}
                        </p>
                        <h3>${receiptData.staticTexts.paymentMethodSectionTitle.toUpperCase(Locale.getDefault())}</h3>
                        <p>
                            <span class="card-icon ${buildIconClass(receiptData.receiptPaymentInfo.cardInfo.brand)}">
                            </span> - ${receiptData.receiptPaymentInfo.cardInfo.last4CardDigits}
                        </p>
                    </header>
                    <h3>${receiptData.staticTexts.summarySectionTitle.toUpperCase(Locale.getDefault())}</h3>
                    ${buildSummaryTable(receiptData)}
                    <footer>
                        <p>
                            ${buildRequiredItems(receiptData)}
                        </p>
                    </footer>
                </body>
            </html>
        """
    }

    private fun buildIconCSS(cardBrand: PaymentCardBrand): String {
        return ".${buildIconClass(cardBrand)} { " +
            "background-image: url(\"data:image/svg+xml;base64,${cardBrand.base64Data}\") " +
            "}"
    }

    private fun buildIconClass(cardBrand: PaymentCardBrand) = "${cardBrand.iconName}-icon"

    private fun buildSummaryTable(receiptData: ReceiptData): String {
        val builder = StringBuilder()
        builder.append("<table>")
        receiptData.purchasedProducts.forEach { item ->
            builder.append(
                "<tr><td>${item.title} &#215; ${formatFloat(item.quantity)}</td>"
            )
                .append("<td>${"%.2f".format(item.itemsTotalAmount)} ")
                .append("${receiptData.receiptPaymentInfo.currency}</td></tr>")
        }
        builder.append(
            """
                <tr>
                    <td>
                        ${receiptData.staticTexts.amountPaidSectionTitle}
                    </td>
                    <td>
                        ${"%.2f".format(receiptData.receiptPaymentInfo.chargedAmount)} ${receiptData.receiptPaymentInfo.currency}
                    </td>
                </tr>
            """
        )
        builder.append("</table>")
        return builder.toString()
    }

    // TODO cardreader Add support for other countries (eg. use a factory to get something like RequiredItemsBuilder)
    private fun buildRequiredItems(receiptData: ReceiptData): String {
        val applicationPreferredName = receiptData.receiptPaymentInfo.applicationPreferredName
        val dedicatedFileName = receiptData.receiptPaymentInfo.dedicatedFileName

        /*
            According to the documentation, only `Application name` and dedicatedFileName (aka AID)
            are required in the US (see https://stripe.com/docs/terminal/checkout/receipts#custom)
        */
        return """
            ${receiptData.staticTexts.applicationName}: $applicationPreferredName<br/>
            ${receiptData.staticTexts.aid}: $dedicatedFileName
        """
    }

    private fun buildReceiptTitle(receiptData: ReceiptData): String {
        val storeName = receiptData.storeName ?: return receiptData.staticTexts.receiptTitle
        return String.format(receiptData.staticTexts.receiptFromFormat, storeName)
    }

    /**
     * Shows decimal places only when the number is not an integer
     */
    private fun formatFloat(number: Float): String =
        if (number.rem(1f).equals(0f)) number.toInt().toString() else "%.2f".format(number)

    private companion object {
        private const val MARGIN: Int = 16
        private const val TITLE_FONT_SIZE: Int = 24
        private const val FONT_SIZE: Int = 12
        private const val FOOTER_FONT_SIZE: Int = 10
        private const val LINE_HEIGHT = FONT_SIZE * 1.5
        private const val ICON_HEIGHT = LINE_HEIGHT
        private const val ICON_WIDTH = ICON_HEIGHT * 4 / 3
    }
}
