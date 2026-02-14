package com.woocommerce.android.ui.login.wpcom

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.login.wpcom.components.WPComConsent
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge

@Composable
fun WPComLoginEmailScreen(viewModel: WPComLoginEmailViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        WPComLoginEmailScreen(
            viewState = it,
            onEmailChanged = viewModel::onEmailOrUsernameChanged,
            onCloseClick = viewModel::onCloseClick,
            onContinueClick = viewModel::onContinueClick
        )
    }
}

@Composable
fun WPComLoginEmailScreen(
    viewState: WPComLoginEmailViewModel.ViewState,
    onEmailChanged: (String) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val branding = viewState.resolveBranding()

    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = ImageVector.vectorResource(branding.navIcon)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (viewState.wpComLoginMode is WPComLoginMode.PushNotificationsSetup) {
                    WordPressWooBadge()
                } else {
                    JetpackToWooHeader()
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(id = branding.title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(id = branding.subtitle))
                Spacer(modifier = Modifier.height(16.dp))

                WCOutlinedTextField(
                    value = viewState.emailOrUsername,
                    onValueChange = onEmailChanged,
                    label = if (viewState.usernameOnly) {
                        stringResource(R.string.username)
                    } else {
                        stringResource(id = R.string.email_or_username)
                    },
                    isError = viewState.errorMessage != null,
                    helperText = viewState.errorMessage?.let { stringResource(id = it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onContinueClick()
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!viewState.usernameOnly) {
                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        text = stringResource(id = branding.helperText)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            WCColoredButton(
                onClick = {
                    keyboardController?.hide()
                    onContinueClick()
                },
                enabled = viewState.enableSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = stringResource(id = branding.buttonText))
            }
            WPComConsent(
                forJetpackSetup = viewState.wpComLoginMode is WPComLoginMode.JetpackSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.checking_email))
    }
}

private data class EmailScreenBranding(
    @DrawableRes val navIcon: Int,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val helperText: Int,
    @StringRes val buttonText: Int,
)

private fun WPComLoginEmailViewModel.ViewState.resolveBranding(): EmailScreenBranding {
    return when (wpComLoginMode) {
        WPComLoginMode.PushNotificationsSetup -> EmailScreenBranding(
            navIcon = R.drawable.ic_back_24dp,
            title = R.string.login_wpcom_connect_title,
            subtitle = R.string.login_wpcom_connect_subtitle,
            helperText = R.string.login_wpcom_connect_create_account_hint,
            buttonText = R.string.continue_button,
        )

        is WPComLoginMode.JetpackSetup -> EmailScreenBranding(
            navIcon = R.drawable.ic_close_24dp,
            title = if (wpComLoginMode.jetpackStatus.isJetpackInstalled) {
                R.string.login_jetpack_connect
            } else {
                R.string.login_jetpack_install
            },
            subtitle = if (wpComLoginMode.jetpackStatus.isJetpackInstalled) {
                R.string.login_jetpack_connection_enter_wpcom_email
            } else {
                R.string.login_jetpack_installation_enter_wpcom_email
            },
            helperText = R.string.login_jetpack_connection_create_account,
            buttonText = if (wpComLoginMode.jetpackStatus.isJetpackInstalled) {
                R.string.login_jetpack_connect
            } else {
                R.string.login_jetpack_install
            },
        )
    }
}

@Preview()
@Composable
private fun JetpackModePreview() {
    WooThemeWithBackground {
        WPComLoginEmailScreen(
            viewState = WPComLoginEmailViewModel.ViewState(
                wpComLoginMode = WPComLoginMode.JetpackSetup(
                    JetpackStatus(
                        isJetpackInstalled = false,
                        jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                            siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                            blogId = null
                        )
                    )
                ),
                usernameOnly = false,
                emailOrUsername = "",
            )
        )
    }
}

@Preview()
@Composable
private fun NotificationSetupModePreview() {
    WooThemeWithBackground {
        WPComLoginEmailScreen(
            viewState = WPComLoginEmailViewModel.ViewState(
                wpComLoginMode = WPComLoginMode.PushNotificationsSetup,
                usernameOnly = false,
                emailOrUsername = "",
            )
        )
    }
}
