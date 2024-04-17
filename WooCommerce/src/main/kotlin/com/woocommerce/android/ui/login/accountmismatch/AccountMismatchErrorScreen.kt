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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
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
import com.woocommerce.android.util.ChromeCustomTabUtils
import org.wordpress.android.fluxc.network.UserAgent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccountMismatchErrorScreen(viewModel: AccountMismatchErrorViewModel) {
    val webViewNavigator = rememberWebViewNavigator()

    viewModel.viewState.observeAsState().value?.let { viewState ->
        BackHandler(onBack = viewState.onBackPressed)

        Scaffold(topBar = {
            ToolbarWithHelpButton(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = {
                    if (webViewNavigator.canGoBack) {
                        webViewNavigator.navigateBack()
                    } else {
                        viewState.onBackPressed()
                    }
                },
                onHelpButtonClick = viewModel::onHelpButtonClick
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
    modifier: Modifier = Modifier
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
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

        if (viewState.showJetpackTermsConsent) {
            val consent = annotatedStringRes(stringResId = R.string.login_jetpack_connection_consent)
            val context = LocalContext.current
            ClickableText(
                text = consent,
                style = MaterialTheme.typography.caption.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface
                )
            ) {
                consent.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                    .firstOrNull()
                    ?.let { annotation ->
                        when (annotation.item) {
                            "terms" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.WORPRESS_COM_TERMS)
                            "sync" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.JETPACK_SYNC_POLICY)
                        }
                    }
            }
        }
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
            painter = painterResource(id = R.drawable.img_woo_generic_error),
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
                inlineButtonAction = {},
                showJetpackTermsConsent = true,
                onBackPressed = {}
            )
        )
    }
}

@Preview
@Composable
private fun SiteCredentialsScreenPreview() {
    WooThemeWithBackground {
        SiteCredentialsScreen(
            viewState = ViewState.SiteCredentialsViewState(
                siteUrl = "woocommerce.com",
                username = "username",
                password = "password",
                errorMessage = null,
                onUsernameChanged = {},
                onPasswordChanged = {},
                onContinueClick = {},
                onLoginWithAnotherAccountClick = {},
                onBackPressed = {}
            )
        )
    }
}
