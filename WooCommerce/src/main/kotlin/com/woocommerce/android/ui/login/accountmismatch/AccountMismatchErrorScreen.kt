package com.woocommerce.android.ui.login.accountmismatch

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.ViewState

@Composable
fun AccountMismatchErrorScreen(viewModel: AccountMismatchErrorViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                title = { },
                navigationIcon = {
                    if (viewState is ViewState.JetpackWebViewState) {
                        IconButton(onClick = viewState.onDismiss) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onHelpButtonClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_help_24dp),
                            contentDescription = stringResource(id = R.string.help)
                        )
                    }
                },
                elevation = 0.dp
            )
        }) { paddingValues ->
            when (viewState) {
                is ViewState.MainState -> AccountMismatchErrorScreen(
                    viewState = viewState,
                    modifier = Modifier.padding(paddingValues)
                )
                is ViewState.JetpackWebViewState -> JetpackConnectionWebView(
                    viewState = viewState,
                    modifier = Modifier.padding(paddingValues)
                )
                ViewState.FetchingJetpackEmailViewState -> FetchJetpackEmailScreen(
                    modifier = Modifier.padding(paddingValues)
                )
                is ViewState.JetpackEmailErrorState -> JetpackEmailErrorScreen(
                    modifier = Modifier.padding(paddingValues),
                    retry = viewState.retry
                )
            }
        }
    }
    viewModel.loadingDialogMessage.observeAsState().value?.let {
        ProgressDialog(title = "", subtitle = stringResource(id = it))
    }
}

@Composable
fun AccountMismatchErrorScreen(viewState: ViewState.MainState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        MainContent(
            viewState = viewState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )

        ButtonBar(
            primaryButtonText = viewState.primaryButtonText?.let { stringResource(id = it) },
            primaryButtonClick = viewState.primaryButtonAction,
            secondaryButtonText = stringResource(id = viewState.secondaryButtonText),
            secondaryButtonClick = viewState.secondaryButtonAction,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MainContent(
    viewState: ViewState.MainState,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        viewState.userInfo?.let {
            UserInfo(it)
        }

        Image(
            painter = painterResource(id = R.drawable.img_woo_no_stores),
            contentDescription = null,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .weight(1f, fill = false)
        )
        Text(
            text = viewState.message,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        viewState.inlineButtonText?.let { buttonText ->
            WCTextButton(onClick = viewState.inlineButtonAction) {
                Text(text = stringResource(id = buttonText))
            }
        }
    }
}

@Composable
private fun UserInfo(userInfo: AccountMismatchErrorViewModel.UserInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = Builder(LocalContext.current)
                .data(userInfo.avatarUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.img_gravatar_placeholder),
            error = painterResource(R.drawable.img_gravatar_placeholder),
            contentDescription = stringResource(R.string.login_avatar_content_description),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .size(dimensionResource(R.dimen.image_major_72))
                .clip(CircleShape)
        )

        Text(
            text = userInfo.displayName,
            style = MaterialTheme.typography.h5,
            color = colorResource(id = R.color.color_on_surface_high)
        )
        Text(
            text = userInfo.username,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun ButtonBar(
    primaryButtonText: String?,
    primaryButtonClick: () -> Unit,
    secondaryButtonText: String,
    secondaryButtonClick: () -> Unit,
    modifier: Modifier
) {
    @Composable
    fun Buttons(modifier: Modifier) {
        primaryButtonText?.let {
            WCColoredButton(onClick = primaryButtonClick, modifier = modifier) {
                Text(text = primaryButtonText)
            }
        }

        WCOutlinedButton(onClick = secondaryButtonClick, modifier = modifier) {
            Text(text = secondaryButtonText)
        }
    }

    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Buttons(modifier = Modifier.weight(1f))
        }
        else -> Column(modifier = modifier) {
            Buttons(modifier = Modifier.fillMaxWidth())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun JetpackConnectionWebView(viewState: ViewState.JetpackWebViewState, modifier: Modifier = Modifier) {
    var progress by remember {
        mutableStateOf(0)
    }
    Column(modifier = modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = (progress / 100f),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (progress == 100) 0f else 1f)
        )

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    this.webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            url?.let { decideUrl(it) }
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            url?.let { decideUrl(it) }
                        }

                        private fun decideUrl(url: String) {
                            val urlWithoutScheme = url.replace("^https?://".toRegex(), "")
                            if (viewState.successConnectionUrls.any { urlWithoutScheme.startsWith(it) }) {
                                viewState.onConnected()
                            }
                        }
                    }
                    this.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    this.settings.javaScriptEnabled = true

                    loadUrl(viewState.connectionUrl)
                }
            }
        )
    }
}

@Composable
private fun FetchJetpackEmailScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.login_jetpack_verify_connection),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        CircularProgressIndicator()
    }
}

@Composable
private fun JetpackEmailErrorScreen(retry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.major_100), Alignment.CenterVertically)
    ) {
        Image(painter = painterResource(id = R.drawable.img_wpcom_error), contentDescription = null)
        Text(
            text = stringResource(id = R.string.login_jetpack_connection_verification_failed),
            textAlign = TextAlign.Center
        )
        WCColoredButton(onClick = retry) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}

@Preview
@Composable
private fun AccountMismatchPreview() {
    WooThemeWithBackground {
        AccountMismatchErrorScreen(
            viewState = ViewState.MainState(
                userInfo = AccountMismatchErrorViewModel.UserInfo(
                    displayName = "displayname",
                    username = "username",
                    avatarUrl = ""
                ),
                message = stringResource(id = R.string.login_wpcom_account_mismatch, "url"),
                primaryButtonText = R.string.continue_button,
                primaryButtonAction = {},
                secondaryButtonText = R.string.continue_button,
                secondaryButtonAction = {},
                inlineButtonText = R.string.continue_button,
                inlineButtonAction = {}
            )
        )
    }
}
