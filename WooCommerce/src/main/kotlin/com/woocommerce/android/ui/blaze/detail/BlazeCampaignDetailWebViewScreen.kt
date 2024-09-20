package com.woocommerce.android.ui.blaze.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.web.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun BlazeCampaignDetailWebViewScreen(
    viewModel: BlazeCampaignDetailWebViewViewModel,
    wpComAuthenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_details_title),
                onNavigationButtonClick = onDismiss
            )
        }
    ) { paddingValues ->
        WCWebView(
            url = viewModel.urlToLoad,
            userAgent = userAgent,
            wpComAuthenticator = wpComAuthenticator,
            onUrlLoaded = onUrlLoaded,
            clearCache = true,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
