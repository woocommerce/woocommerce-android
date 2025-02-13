package com.woocommerce.android.ui.login.jetpack.connection

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.web.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun JetpackActivationWebViewScreen(
    viewModel: JetpackActivationWebViewViewModel,
    authenticator: WebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    onUrlFailed: (String, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.login_jetpack_installation_approve_connection),
                onNavigationButtonClick = onDismiss,
                navigationIcon = Icons.Filled.Clear
            )
        }
    ) { paddingValues ->
        WCWebView(
            url = viewModel.urlToLoad,
            userAgent = userAgent,
            authenticator = authenticator,
            onUrlLoaded = onUrlLoaded,
            onUrlFailed = onUrlFailed,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
