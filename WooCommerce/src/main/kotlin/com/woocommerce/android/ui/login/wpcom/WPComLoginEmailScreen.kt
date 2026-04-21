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
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.login.wpcom.components.WPComConsent

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
                .verticalScroll(rememberScrollState())
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        id = if (viewState.isJetpackInstalled) {
                            R.string.login_jetpack_connection_enter_wpcom_email
                        } else {
                            R.string.login_jetpack_installation_enter_wpcom_email
                        }
                    )
                )
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
                        text = stringResource(id = R.string.login_jetpack_connection_create_account)
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
                Text(text = stringResource(id = titleRes))
            }
            WPComConsent(
                forJetpackSetup = true,
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

@Preview
@Composable
private fun JetpackModePreview() {
    WooThemeWithBackground {
        WPComLoginEmailScreen(
            viewState = WPComLoginEmailViewModel.ViewState(
                isJetpackInstalled = false,
                usernameOnly = false,
                emailOrUsername = "",
            )
        )
    }
}

