package com.woocommerce.android.ui.payments.receipt.preview

import javax.inject.Inject

class ReceiptHtmlInterceptor @Inject constructor() {

    fun interceptHtmlContent(originalHtml: String): String {
        return if (originalHtml.contains("<head>")) {
            originalHtml.replace(
                "<head>",
                "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            )
        } else {
            originalHtml
        }
    }
}
