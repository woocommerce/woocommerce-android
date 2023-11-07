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
import androidx.compose.material.icons.Icons.Filled
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
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackConsent
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader

@Composable
fun JetpackActivationWPComEmailScreen(viewModel: JetpackActivationWPComEmailViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationWPComEmailScreen(
            viewState = it,
            onEmailChanged = viewModel::onEmailOrUsernameChanged,
            onCloseClick = viewModel::onCloseClick,
            onContinueClick = viewModel::onContinueClick
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JetpackActivationWPComEmailScreen(
    viewState: JetpackActivationWPComEmailViewModel.ViewState,
    onEmailChanged: (String) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = Filled.Clear
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
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(
                        id = if (viewState.isJetpackInstalled) {
                            R.string.login_jetpack_connection_enter_wpcom_email
                        } else {
                            R.string.login_jetpack_installation_enter_wpcom_email
                        }
                    )
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                WCOutlinedTextField(
                    value = viewState.emailOrUsername,
                    onValueChange = onEmailChanged,
                    label = stringResource(id = R.string.email_or_username),
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
                        id = if (viewState.isJetpackInstalled) R.string.login_jetpack_connect
                        else R.string.login_jetpack_install
                    )
                )
            }
            JetpackConsent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.checking_email))
    }
}

@Preview(heightDp = 130)
@Composable
private fun JetpackActivationWPComScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationWPComEmailScreen(
            viewState = JetpackActivationWPComEmailViewModel.ViewState(
                emailOrUsername = "",
                isJetpackInstalled = false
            )
        )
    }
}
