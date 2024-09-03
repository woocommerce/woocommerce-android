package com.woocommerce.android.ui.compose.component.web

import android.content.ActivityNotFoundException
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

class WebChromeClientWithImageChooser(
    registry: ActivityResultRegistry,
    private val onProgressChanged: (Int) -> Unit
) : WebChromeClient() {
    companion object {
        private const val FILE_CHOOSER_RESULT_KEY = "file_chooser_result_key"
    }

    private lateinit var fileChooserValueCallback: ValueCallback<Array<Uri>>
    private val getImageContent = registry.register(FILE_CHOOSER_RESULT_KEY, GetContent()) { uri ->
        uri?.let {
            fileChooserValueCallback.onReceiveValue(arrayOf(uri))
        } ?: fileChooserValueCallback.onReceiveValue(null)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        try {
            fileChooserValueCallback = filePathCallback
            getImageContent.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            WooLog.d(
                T.UTILS,
                "WebChromeClientWithImageChooser. No activity found to handle image selection: ${e.message}"
            )
        }
        return true
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }
}
