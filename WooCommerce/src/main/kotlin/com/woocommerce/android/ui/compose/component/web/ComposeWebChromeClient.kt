package com.woocommerce.android.ui.compose.component.web

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.woocommerce.android.extensions.findActivity

open class ComposeWebChromeClient : WebChromeClient() {
    var onProgressChanged: (Int) -> Unit = {}

    /**
     * This method is called when the user chooses a file for file upload.
     *
     * Important: this implementation doesn't handle configuration changes, during which the flow
     * will just be interrupted.
     * Our WebView implementation doesn't survive configuration changes either, so this is fine for now.
     *
     * If we need to handle configuration changes, we'll need the following:
     * - Find a way to keep the WebView instance across configuration changes.
     * - Store the [filePathCallback] during configuration changes (possibly in a ViewModel).
     * - Move the ActivityResultLauncher outside of this method to make sure it's called after the configuration change.
     */
    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        val activity = webView.context.findActivity() as? ComponentActivity ?: return false

        val contract = ActivityResultContracts.StartActivityForResult()
        val launcher = activity.activityResultRegistry.register(
            "WebViewChooser",
            contract
        ) { result ->
            val uris = FileChooserParams.parseResult(result.resultCode, result.data)
            filePathCallback.onReceiveValue(uris)
        }

        val intent = fileChooserParams.createIntent()
        launcher.launch(intent)

        return true
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }
}
