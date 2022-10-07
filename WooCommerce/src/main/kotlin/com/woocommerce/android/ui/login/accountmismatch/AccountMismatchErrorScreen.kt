package com.woocommerce.android.ui.login.accountmismatch

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.component.WebViewNavigator
import com.woocommerce.android.ui.compose.component.rememberWebViewNavigator
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.ViewState
import org.wordpress.android.fluxc.network.UserAgent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccountMismatchErrorScreen(viewModel: AccountMismatchErrorViewModel) {
    val webViewNavigator = rememberWebViewNavigator()

    viewModel.viewState.observeAsState().value?.let { viewState ->
        val overrideBackButton by derivedStateOf {
            viewState is ViewState.JetpackWebViewState ||
                viewState is ViewState.SiteCredentialsViewState
        }
        val goBack = {
            when (val state = viewModel.viewState.value) {
                is ViewState.JetpackWebViewState -> state.onDismiss()
                is ViewState.SiteCredentialsViewState -> state.onCancel()
                else -> {
                    // NO-OP
                }
            }
        }

        BackHandler(overrideBackButton, onBack = goBack)

        Scaffold(topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                title = { },
                navigationIcon = {
                    if (overrideBackButton) {
                        IconButton(onClick = {
                            if (webViewNavigator.canGoBack) {
                                webViewNavigator.navigateBack()
                            } else {
                                goBack()
                            }
                        }) {
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
            val transition = updateTransition(targetState = viewState, label = "state")
            transition.AnimatedContent(
                contentKey = { viewState::class.java }
            ) { targetState ->
                when (targetState) {
                    is ViewState.MainState -> AccountMismatchErrorScreen(
                        viewState = targetState,
                        modifier = Modifier.padding(paddingValues)
                    )
                    is ViewState.JetpackWebViewState -> JetpackConnectionWebView(
                        viewState = targetState,
                        wpComWebViewAuthenticator = viewModel.wpComWebViewAuthenticator,
                        webViewNavigator = webViewNavigator,
                        userAgent = viewModel.userAgent,
                        modifier = Modifier.padding(paddingValues)
                    )
                    ViewState.FetchingJetpackEmailViewState -> FetchJetpackEmailScreen(
                        modifier = Modifier.padding(paddingValues)
                    )
                    is ViewState.JetpackEmailErrorState -> JetpackEmailErrorScreen(
                        modifier = Modifier.padding(paddingValues),
                        retry = targetState.retry
                    )
                    is ViewState.SiteCredentialsViewState -> SiteCredentialsScreen(
                        viewState = targetState,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
    viewModel.loadingDialogMessage.observeAsState().value?.let {
        ProgressDialog(title = "", subtitle = stringResource(id = it))
    }
}

@Composable
private fun SiteCredentialsScreen(
    viewState: ViewState.SiteCredentialsViewState,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.enter_credentials_for_site, viewState.siteUrl))
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            WCOutlinedTextField(
                value = viewState.username,
                onValueChange = viewState.onUsernameChanged,
                label = stringResource(id = R.string.username),
                isError = viewState.errorMessage != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            WCPasswordField(
                value = viewState.password,
                onValueChange = viewState.onPasswordChanged,
                label = stringResource(id = R.string.password),
                isError = viewState.errorMessage != null,
                helperText = viewState.errorMessage?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { viewState.onContinueClick() }
                )
            )
        }

        ButtonBar(
            primaryButtonText = stringResource(id = R.string.continue_button),
            primaryButtonClick = viewState.onContinueClick,
            isPrimaryButtonEnabled = viewState.isValid,
            secondaryButtonText = stringResource(id = R.string.login_try_another_account),
            secondaryButtonClick = viewState.onLoginWithAnotherAccountClick,
            modifier = Modifier.fillMaxWidth()
        )
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
    modifier: Modifier = Modifier,
    isPrimaryButtonEnabled: Boolean = true,
) {
    @Composable
    fun Buttons(modifier: Modifier) {
        primaryButtonText?.let {
            WCColoredButton(onClick = primaryButtonClick, enabled = isPrimaryButtonEnabled, modifier = modifier) {
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
private fun JetpackConnectionWebView(
    viewState: ViewState.JetpackWebViewState,
    wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    webViewNavigator: WebViewNavigator,
    userAgent: UserAgent,
    modifier: Modifier = Modifier
) {
    WCWebView(
        url = viewState.connectionUrl,
        wpComAuthenticator = wpComWebViewAuthenticator,
        userAgent = userAgent,
        webViewNavigator = webViewNavigator,
        onUrlLoaded = { url: String ->
            val urlWithoutScheme = url.replace("^https?://".toRegex(), "")
            if (viewState.successConnectionUrls.any { urlWithoutScheme.startsWith(it) }) {
                viewState.onConnected()
            }
        },
        modifier = modifier.fillMaxSize()
    )
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
