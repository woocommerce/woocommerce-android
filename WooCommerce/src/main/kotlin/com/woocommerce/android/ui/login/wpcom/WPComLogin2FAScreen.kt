package com.woocommerce.android.ui.login.wpcom

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
import androidx.compose.material3.Icon
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
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader

@Composable
fun WPComLogin2FAScreen(viewModel: WPComLogin2FAViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        WPComLogin2FAScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick,
            onSmsButtonClick = viewModel::onSmsButtonClick,
            onContinueClick = viewModel::onContinueClick,
            onOTPChanged = viewModel::onOTPChanged,
            onSecurityKeyClick = viewModel::onSecurityKeyClick
        )
    }
}

@Composable
fun WPComLogin2FAScreen(
    viewState: WPComLogin2FAViewModel.ViewState,
    onCloseClick: () -> Unit = {},
    onSmsButtonClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onOTPChanged: (String) -> Unit = {},
    onSecurityKeyClick: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleRes = if (viewState.isJetpackInstalled) {
        R.string.login_jetpack_connect
    } else {
        R.string.login_jetpack_install
    }

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
                JetpackToWooHeader()
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(id = titleRes),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        id = if (viewState.hasRequestedSms) {
                            R.string.enter_verification_code_sms_generic
                        } else {
                            R.string.enter_verification_code
                        }
                    )
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
                Spacer(modifier = Modifier.height(16.dp))
                WCOutlinedButton(
                    onClick = onSmsButtonClick,
                    text = stringResource(
                        id = if (viewState.hasRequestedSms) {
                            R.string.login_text_otp_another
                        } else {
                            R.string.login_text_otp
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_comment),
                            contentDescription = null
                        )
                    },
                    enabled = viewState.canUseAlternateMethods,
                    loading = viewState.isRequestingSms
                )
                if (viewState.isSecurityKeySupported) {
                    WCTextButton(
                        onClick = onSecurityKeyClick,
                        enabled = viewState.canUseAlternateMethods
                    ) {
                        Text(text = stringResource(id = R.string.login_text_security_key))
                    }
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
                Text(text = stringResource(id = titleRes))
            }
        }
    }

    viewState.loadingMessage?.let {
        ProgressDialog(title = "", subtitle = stringResource(id = it))
    }
}

@Preview
@Composable
private fun JetpackModePreview() {
    WooThemeWithBackground {
        WPComLogin2FAScreen(
            viewState = WPComLogin2FAViewModel.ViewState(
                isJetpackInstalled = false,
                emailOrUsername = "test@email.com",
                password = "",
                otp = "123456"
            )
        )
    }
}
