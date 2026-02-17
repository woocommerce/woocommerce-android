package com.woocommerce.android.ui.login.wpcom

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
import androidx.compose.ui.text.input.KeyboardType
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
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_close_24dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.enter_verification_code)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
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
        }
    }

    viewState.loadingMessage?.let {
        ProgressDialog(title = "", subtitle = stringResource(id = it))
    }
}

private data class TwoFAScreenBranding(
    @StringRes val title: Int,
    @StringRes val buttonText: Int,
)

private fun WPComLogin2FAViewModel.ViewState.resolveBranding(): TwoFAScreenBranding {
    return when (wpComLoginMode) {
        WPComLoginMode.PushNotificationsSetup -> TwoFAScreenBranding(
            title = R.string.login_wpcom_connect_title,
            buttonText = R.string.login_wpcom_connect_title,
        )

        is WPComLoginMode.JetpackSetup -> TwoFAScreenBranding(
            title = if (wpComLoginMode.jetpackStatus.isJetpackInstalled) {
                R.string.login_jetpack_connect
            } else {
                R.string.login_jetpack_install
            },
            buttonText = if (wpComLoginMode.jetpackStatus.isJetpackInstalled) {
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
                    JetpackStatus(
                        isJetpackInstalled = false,
                        jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                            siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                            blogId = null
                        )
                    )
                ),
                emailOrUsername = "test@email.com",
                password = "",
                otp = "123456"
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
                otp = "123456"
            )
        )
    }
}
