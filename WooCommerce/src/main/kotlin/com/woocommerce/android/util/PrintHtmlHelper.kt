package com.woocommerce.android.util

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.WooLog.T
import javax.inject.Inject

class PrintHtmlHelper @Inject constructor() {
    // Hold an instance of the WebView object so it isn't garbage collected before the print job is created
    private var webViewInstance: WebView? = null
    private var printJob: PrintJob? = null

    fun printReceipt(activity: Activity, receiptUrl: String, documentName: String) {
        webViewInstance?.let {
            WooLog.e(
                T.UTILS,
                "Initiating print job before the previous job has finished. " +
                    "The previous job might fail since its WebView might get garbage collected."
            )
        }
        val webView = WebView(activity)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                enqueuePrintJob(activity, view, documentName)
                webViewInstance = null
            }
        }

        webView.loadUrl(receiptUrl)
        webViewInstance = webView
    }

    fun getAndClearPrintJobResult(): PrintJobResult? {
        return printJob?.let {
            when {
                it.isCancelled -> CANCELLED
                it.isFailed -> FAILED
                else -> PrintJobResult.STARTED
            }.also { printJob = null }
        }
    }

    private fun enqueuePrintJob(activity: Activity, webView: WebView, documentName: String) {
        (activity.getSystemService(Context.PRINT_SERVICE) as PrintManager).print(
            documentName,
            webView.createPrintDocumentAdapter(documentName),
            PrintAttributes.Builder().build()
        ).also { printJob = it }
    }

    enum class PrintJobResult {
        CANCELLED, STARTED, FAILED
    }
}
