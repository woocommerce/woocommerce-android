package com.woocommerce.android.ui.login.jetpack.wpcom

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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

@Composable
fun JetpackActivationWPCom2FAScreen(viewModel: JetpackActivationWPCom2FAViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationWPCom2FAScreen(
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
fun JetpackActivationWPCom2FAScreen(
    viewState: JetpackActivationWPCom2FAViewModel.ViewState,
    onCloseClick: () -> Unit = {},
    onSMSLinkClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onOTPChanged: (String) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = Icons.Filled.Clear
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
                JetpackToWooHeader()
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
                val title = if (viewState.isJetpackInstalled) {
                    R.string.login_jetpack_connect
                } else {
                    R.string.login_jetpack_install
                }
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(
                        id = R.string.enter_verification_code
                    )
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
                Text(
                    text = stringResource(
                        id = if (viewState.isJetpackInstalled) {
                            R.string.login_jetpack_connect
                        } else {
                            R.string.login_jetpack_install
                        }
                    )
                )
            }
        }
    }

    viewState.loadingMessage?.let {
        ProgressDialog(title = "", subtitle = stringResource(id = it))
    }
}

@Preview
@Composable
private fun JetpackActivationWPCom2FAScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationWPCom2FAScreen(
            viewState = JetpackActivationWPCom2FAViewModel.ViewState(
                emailOrUsername = "test@email.com",
                password = "",
                otp = "123456",
                isJetpackInstalled = false
            )
        )
    }
}
