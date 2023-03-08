package com.woocommerce.android.ui.login.storecreation.installation

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.drawShadow
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorScreen
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.InitialState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun InstallationScreen(
    viewModel: InstallationViewModel
) {
    viewModel.viewState.observeAsState(InitialState).value.let { state ->
        Crossfade(targetState = state) { viewState ->
            when (viewState) {
                is SuccessState -> InstallationSummary(
                    viewState.url,
                    viewModel::onManageStoreButtonClicked,
                    viewModel::onShowPreviewButtonClicked,
                    viewModel.wpComWebViewAuthenticator,
                    viewModel.userAgent,
                    viewModel::onUrlLoaded
                )
                is ErrorState -> StoreCreationErrorScreen(
                    viewState.errorType,
                    viewModel::onBackPressed,
                    viewState.message,
                    viewModel::onRetryButtonClicked
                )
                is InitialState, LoadingState -> {
                    ProgressIndicator(stringResource(id = string.store_creation_in_progress))
                }
            }
        }
    }
}

@Composable
private fun InstallationSummary(
    url: String,
    onManageStoreButtonClicked: () -> Unit,
    onShowPreviewButtonClicked: () -> Unit,
    authenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.store_creation_installation_success),
            color = colorResource(id = color.color_on_surface),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .padding(
                    top = dimensionResource(id = dimen.major_350)
                )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .background(color = colorResource(id = color.color_surface))
                .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_100)))
                .padding(
                    horizontal = dimensionResource(id = dimen.major_350),
                    vertical = dimensionResource(id = dimen.major_200)
                )
        ) {
            PreviewWebView(
                url = url,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .drawShadow(
                        color = colorResource(id = color.color_on_surface),
                        backgroundColor = colorResource(id = color.color_surface),
                        borderRadius = dimensionResource(id = dimen.major_100)
                    ),
                authenticator = authenticator,
                userAgent = userAgent,
                onUrlLoaded = onUrlLoaded
            )
        }

        Divider(
            color = colorResource(id = color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10),
            modifier = Modifier.padding(bottom = dimensionResource(id = dimen.major_100))
        )

        WCColoredButton(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = dimen.major_100))
                .fillMaxWidth(),
            onClick = onManageStoreButtonClicked
        ) {
            Text(
                text = stringResource(id = string.store_creation_installation_manage_store_button)
            )
        }

        WCOutlinedButton(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = dimen.major_100),
                    end = dimensionResource(id = dimen.major_100),
                    bottom = dimensionResource(id = dimen.major_100)
                )
                .fillMaxWidth(),
            onClick = onShowPreviewButtonClicked
        ) {
            Text(
                text = stringResource(id = string.store_creation_installation_show_preview_button)
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun PreviewWebView(
    url: String,
    modifier: Modifier = Modifier,
    authenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit
) {
    var progress by remember { mutableStateOf(0) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .padding(dimensionResource(id = dimen.minor_100))
            .fillMaxSize()
            .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_100)))
            .border(
                dimensionResource(id = dimen.minor_10),
                colorResource(id = color.gray_0),
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100)),
            )
    ) {

        Column(modifier = Modifier.align(Alignment.Center)) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .alpha(if (progress == 100) 0f else 1f)
                    .padding(dimensionResource(id = dimen.major_100)),
            )
            Text(text = stringResource(id = string.store_creation_installation_rendering_preview_label))
        }
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                    this.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (progress == 100) {
                                view?.settings?.javaScriptEnabled = false
                            }
                        }
                    }
                    this.webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            url?.let { onUrlLoaded(it) }
                        }
                    }

                    this.setInitialScale(140)
                    this.setOnTouchListener { _, _ -> true }
                    this.settings.javaScriptEnabled = true
                    this.settings.loadWithOverviewMode = true
                    this.settings.userAgentString = userAgent.userAgent
                }
            },
            modifier = Modifier
                .alpha(if (progress == 100) 1f else 0f)
                .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_75)))
        ) { webView ->
            if (lastLoadedUrl == url) return@AndroidView
            lastLoadedUrl = url
            WooLog.d(WooLog.T.ONBOARDING, url)
            authenticator.authenticateAndLoadUrl(webView, url)
        }
    }
}
