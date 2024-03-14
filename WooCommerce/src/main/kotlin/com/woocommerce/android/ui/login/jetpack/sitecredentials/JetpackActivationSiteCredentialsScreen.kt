package com.woocommerce.android.ui.login.jetpack.sitecredentials

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.JetpackActivationSiteCredentialsViewState

@Composable
fun JetpackActivationSiteCredentialsScreen(viewModel: JetpackActivationSiteCredentialsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationSiteCredentialsScreen(
            viewState = it,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onContinueClick = viewModel::onContinueClick,
            onResetPasswordClick = viewModel::onResetPasswordClick,
            onCloseClick = viewModel::onCloseClick
        )
    }
}

@Composable
fun JetpackActivationSiteCredentialsScreen(
    viewState: JetpackActivationSiteCredentialsViewState,
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onContinueClick: () -> Unit = {},
    onResetPasswordClick: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
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
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = annotatedStringRes(
                        stringResId = if (viewState.isJetpackInstalled) {
                            R.string.login_jetpack_connection_enter_site_credentials
                        } else {
                            R.string.login_jetpack_installation_enter_site_credentials
                        },
                        viewState.siteUrl
                    )
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                WCOutlinedTextField(
                    value = viewState.username,
                    onValueChange = onUsernameChanged,
                    label = stringResource(id = R.string.username),
                    isError = viewState.errorMessage != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                WCPasswordField(
                    value = viewState.password,
                    onValueChange = onPasswordChanged,
                    label = stringResource(id = R.string.password),
                    isError = viewState.errorMessage != null,
                    helperText = viewState.errorMessage?.getText(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onContinueClick() }
                    )
                )
                WCTextButton(onClick = onResetPasswordClick) {
                    Text(text = stringResource(id = R.string.reset_your_password))
                }
            }

            WCColoredButton(
                onClick = onContinueClick,
                enabled = viewState.isValid,
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
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }

    if (viewState.isLoading) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.logging_in))
    }
}

@Preview
@Composable
private fun JetpackActivationSiteCredentialsScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationSiteCredentialsScreen(
            viewState = JetpackActivationSiteCredentialsViewState(
                isJetpackInstalled = false,
                siteUrl = "reallyniceshirts.com"
            )
        )
    }
}
