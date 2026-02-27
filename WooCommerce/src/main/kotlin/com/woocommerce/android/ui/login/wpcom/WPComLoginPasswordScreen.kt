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
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.login.wpcom.components.UserInfo

@Composable
fun WPComLoginPasswordScreen(viewModel: WPComLoginPasswordViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        WPComLoginPasswordScreen(
            viewState = it,
            onPasswordChanged = viewModel::onPasswordChanged,
            onCloseClick = viewModel::onCloseClick,
            onContinueClick = viewModel::onContinueClick,
            onMagicLinkClick = viewModel::onMagicLinkClick,
            onResetPasswordClick = viewModel::onResetPasswordClick
        )
    }
}

@Composable
fun WPComLoginPasswordScreen(
    viewState: WPComLoginPasswordViewModel.ViewState,
    onPasswordChanged: (String) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onMagicLinkClick: () -> Unit = {},
    onResetPasswordClick: () -> Unit = {}
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
                UserInfo(
                    emailOrUsername = viewState.emailOrUsername,
                    avatarUrl = viewState.avatarUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        id = if (viewState.isJetpackInstalled) {
                            R.string.login_jetpack_connection_enter_wpcom_password
                        } else {
                            R.string.login_jetpack_installation_enter_wpcom_password
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                WCPasswordField(
                    value = viewState.password,
                    onValueChange = onPasswordChanged,
                    label = stringResource(id = R.string.password),
                    isError = viewState.errorMessage != null,
                    helperText = viewState.errorMessage?.let { stringResource(id = it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (viewState.enableSubmit) {
                                keyboardController?.hide()
                                onContinueClick()
                            }
                        }
                    )
                )
                WCTextButton(onClick = onResetPasswordClick) {
                    Text(text = stringResource(id = R.string.reset_your_password))
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
                Text(text = stringResource(id = titleRes))
            }
            WCOutlinedButton(
                onClick = onMagicLinkClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.login_jetpack_installation_continue_magic_link)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.logging_in))
    }
}

@Preview
@Composable
private fun JetpackModePreview() {
    WooThemeWithBackground {
        WPComLoginPasswordScreen(
            viewState = WPComLoginPasswordViewModel.ViewState(
                isJetpackInstalled = false,
                emailOrUsername = "test@email.com",
                password = "",
                avatarUrl = ""
            )
        )
    }
}

