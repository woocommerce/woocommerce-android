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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge

@Composable
fun WPComLogin2FAScreen(viewModel: WPComLogin2FAViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        WPComLogin2FAScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick,
            onSMSLinkClick = viewModel::onSMSLinkClick,
            onContinueClick = viewModel::onContinueClick,
            onOTPChanged = viewModel::onOTPChanged
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WPComLogin2FAScreen(
    viewState: WPComLogin2FAViewModel.ViewState,
    onCloseClick: () -> Unit = {},
    onSMSLinkClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onOTPChanged: (String) -> Unit = {}
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
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_100)),
            ) {
                if (viewState.wpComLoginMode is WPComLoginMode.PushNotificationsSetup) {
                    WordPressWooBadge()
                } else {
                    JetpackToWooHeader()
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
                Text(
                    text = stringResource(id = branding.title),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(id = R.string.enter_verification_code)
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                WCOutlinedTextField(
                    value = viewState.otp,
                    onValueChange = onOTPChanged,
                    label = stringResource(id = R.string.verification_code),
                    isError = viewState.errorMessage != null,
                    helperText = viewState.errorMessage?.let { stringResource(id = it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onContinueClick()
                        }
                    ),
                    singleLine = true
                )
                WCTextButton(onClick = onSMSLinkClick) {
                    Text(text = stringResource(id = R.string.login_text_otp))
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
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
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(text = stringResource(id = branding.buttonText))
            }
        }
    }

    viewState.loadingMessage?.let {
        ProgressDialog(title = "", subtitle = stringResource(id = it))
    }
}

private data class TwoFAScreenBranding(
    @DrawableRes val navIcon: Int,
    @StringRes val title: Int,
    @StringRes val buttonText: Int,
)

private fun WPComLogin2FAViewModel.ViewState.resolveBranding(): TwoFAScreenBranding {
    return when (wpComLoginMode) {
        WPComLoginMode.PushNotificationsSetup -> TwoFAScreenBranding(
            navIcon = R.drawable.ic_back_24dp,
            title = R.string.login_wpcom_connect_title,
            buttonText = R.string.login_wpcom_connect_title,
        )

        is WPComLoginMode.JetpackSetup -> TwoFAScreenBranding(
            navIcon = R.drawable.ic_close_24dp,
            title = if (isJetpackInstalled) {
                R.string.login_jetpack_connect
            } else {
                R.string.login_jetpack_install
            },
            buttonText = if (isJetpackInstalled) {
                R.string.login_jetpack_connect
            } else {
                R.string.login_jetpack_install
            },
        )
    }
}

@Preview
@Composable
private fun JetpackModePreview() {
    WooThemeWithBackground {
        WPComLogin2FAScreen(
            viewState = WPComLogin2FAViewModel.ViewState(
                wpComLoginMode = WPComLoginMode.JetpackSetup(
                    com.woocommerce.android.model.JetpackStatus(
                        isJetpackInstalled = false,
                        jetpackConnectionStatus = com.woocommerce.android.model.JetpackConnectionStatus
                            .AccountNotConnected(
                                siteRegistrationStatus = com.woocommerce.android.model
                                    .JetpackSiteRegistrationStatus.UNKNOWN,
                                blogId = null
                            )
                    )
                ),
                emailOrUsername = "test@email.com",
                password = "",
                otp = "123456",
                isJetpackInstalled = false
            )
        )
    }
}

@Preview
@Composable
private fun NotificationSetupModePreview() {
    WooThemeWithBackground {
        WPComLogin2FAScreen(
            viewState = WPComLogin2FAViewModel.ViewState(
                wpComLoginMode = WPComLoginMode.PushNotificationsSetup,
                emailOrUsername = "test@email.com",
                password = "",
                otp = "123456",
                isJetpackInstalled = false
            )
        )
    }
}
