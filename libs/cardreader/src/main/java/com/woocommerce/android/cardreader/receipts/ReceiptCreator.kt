package com.woocommerce.android.cardreader.receipts

import java.util.Locale

/**
 * Most parts of this class were copied from woocommerce-ios codebase.
 */
class ReceiptCreator {
    // TODO cardreader ideally move receipt creation to the backend
    fun createHtmlReceipt(paymentData: ReceiptData): String {
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
                    ${buildIconCSS(paymentData.cardInfo.brand)}
                </style>
            </head>
                <body>
                    <header>
                        <h1>${buildReceiptTitle(paymentData)}</h1>
                        <h3>${paymentData.staticTexts.amountPaidSectionTitle.toUpperCase(Locale.getDefault())}</h3>
                        <p>
                            ${paymentData.amount} ${paymentData.currency.toUpperCase(Locale.getDefault())}
                        </p>
                        <h3>${paymentData.staticTexts.datePaidSectionTitle.toUpperCase(Locale.getDefault())}</h3>
                        <p>
                            ${paymentData.receiptDate}
                        </p>
                        <h3>${paymentData.staticTexts.paymentMethodSectionTitle.toUpperCase(Locale.getDefault())}</h3>
                        <p>
                            <span class="card-icon ${buildIconClass(paymentData.cardInfo.brand)}"></span> - ${paymentData.cardInfo.last4CardDigits}
                        </p>
                    </header>
                    <h3>${paymentData.staticTexts.summarySectionTitle.toUpperCase(Locale.getDefault())}</h3>
                    ${buildSummaryTable(paymentData)}
                    <footer>
                        <p>
                            ${buildRequiredItems(paymentData)}
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
            builder.append("<tr><td>${item.title} × ${item.quantity}</td>")
                .append("<td>${item.amount} ${receiptData.currency}</td></tr>")
        }
        builder.append(
            """
                <tr>
                    <td>
                        ${receiptData.staticTexts.amountPaidSectionTitle}
                    </td>
                    <td>
                        ${receiptData.amount} ${receiptData.currency}
                    </td>
                </tr>
            """
        )
        builder.append("</table>")
        return builder.toString()
    }

    // TODO cardreader Add support for other countries (eg. use a factory to get something like RequiredItemsBuilder)
    private fun buildRequiredItems(paymentData: ReceiptData): String {
        val applicationPreferredName = paymentData.applicationPreferredName ?: return "<br/>"
        val dedicatedFileName = paymentData.dedicatedFileName ?: return "<br/>"

        /*
            According to the documentation, only `Application name` and dedicatedFileName (aka AID)
            are required in the US (see https://stripe.com/docs/terminal/checkout/receipts#custom)
        */
        return """
            ${paymentData.staticTexts.applicationName}: $applicationPreferredName<br/>
            ${paymentData.staticTexts.aid}: $dedicatedFileName
        """
    }

    private fun buildReceiptTitle(parameters: ReceiptData): String {
        val storeName = parameters.storeName ?: return parameters.staticTexts.receiptTitle
        return String.format(parameters.staticTexts.receiptFromFormat, storeName)
    }

    companion object {
        const val MARGIN: Int = 16
        const val TITLE_FONT_SIZE: Int = 24
        const val FONT_SIZE: Int = 12
        const val FOOTER_FONT_SIZE: Int = 10
        const val LINE_HEIGHT = FONT_SIZE * 1.5
        const val ICON_HEIGHT = LINE_HEIGHT
        const val ICON_WIDTH = ICON_HEIGHT * 4 / 3
    }
}
