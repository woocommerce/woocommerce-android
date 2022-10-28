package com.woocommerce.android.ui.login.storecreation.webview

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.component.WebViewNavigator
import com.woocommerce.android.ui.compose.component.rememberWebViewNavigator
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.StoreLoadingState
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.network.UserAgent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WebViewStoreCreationScreen(viewModel: WebViewStoreCreationViewModel) {
    val webViewNavigator = rememberWebViewNavigator()

    viewModel.viewState.observeAsState().value?.let { viewState ->
        BackHandler(onBack = viewState.onBackPressed)
        Scaffold(topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                title = { Text(stringResource(id = string.store_creation_create_new_store_label)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewState.onBackPressed()
                    }) {
                        Icon(Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onHelpButtonClick) {
                        Icon(
                            painter = painterResource(id = drawable.ic_help_24dp),
                            contentDescription = stringResource(id = string.help)
                        )
                    }
                },
                elevation = 0.dp
            )
        }) { paddingValues ->
            val transition = updateTransition(targetState = viewState, label = "state")
            transition.AnimatedContent(
                contentKey = { viewState::class.java }
            ) { targetState ->
                when (targetState) {
                    is StoreCreationState -> StoreCreationWebView(
                        viewState = targetState,
                        wpComWebViewAuthenticator = viewModel.wpComWebViewAuthenticator,
                        webViewNavigator = webViewNavigator,
                        userAgent = viewModel.userAgent,
                        modifier = Modifier.padding(paddingValues)
                    )
                    is StoreLoadingState -> StoreCreationInProgress()
                    is ErrorState -> StoreCreationError(targetState)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StoreCreationWebView(
    viewState: StoreCreationState,
    wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    webViewNavigator: WebViewNavigator,
    userAgent: UserAgent,
    modifier: Modifier = Modifier
) {
    fun extractSiteUrlOrNull(url: String): String? {
        return "${viewState.siteUrlKeyword}.+/".toRegex().find(url)?.range?.let { range ->
            val start = range.first + viewState.siteUrlKeyword.length
            val end = range.last
            return url.substring(start, end)
        }
    }

    var storeCreationTriggered: Boolean = false
    WCWebView(
        url = viewState.storeCreationUrl,
        wpComAuthenticator = wpComWebViewAuthenticator,
        userAgent = userAgent,
        webViewNavigator = webViewNavigator,
        onUrlLoaded = { url: String ->
            WooLog.d(T.SITE_PICKER, url)
            extractSiteUrlOrNull(url)?.let {
                viewState.onSiteAddressFound(it)
            }

            if (url.contains(viewState.exitTriggerKeyword, ignoreCase = true) && !storeCreationTriggered) {
                storeCreationTriggered = true
                viewState.onStoreCreated()
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun StoreCreationError(viewState: ErrorState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.store_creation_cannot_load_store),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )

        WCColoredButton(
            onClick = { viewState.onRetryButtonClick() },
            text = stringResource(id = string.retry),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun StoreCreationInProgress() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))

        Text(
            text = stringResource(id = string.store_creation_in_progress),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun PreviewStoreCreationInProgress() {
    StoreCreationInProgress()
}

@Preview
@Composable
private fun PreviewStoreCreationError() {
    StoreCreationError(ErrorState({}, {}))
}
