package com.woocommerce.android.ui.login.sitecredentials

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
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun LoginSiteCredentialsScreen(viewModel: LoginSiteCredentialsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        LoginSiteCredentialsScreen(
            viewState = it,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onContinueClick = viewModel::onContinueClick,
            onResetPasswordClick = viewModel::onResetPasswordClick,
            onBackClick = viewModel::onBackClick,
            onHelpButtonClick = viewModel::onHelpButtonClick,
            onErrorDialogDismissed = viewModel::onErrorDialogDismissed,
            onWebAuthorizationUrlLoaded = viewModel::onWebAuthorizationUrlLoaded
        )
    }
}

@Composable
fun LoginSiteCredentialsScreen(
    viewState: LoginSiteCredentialsViewModel.ViewState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onContinueClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onBackClick: () -> Unit,
    onHelpButtonClick: () -> Unit,
    onErrorDialogDismissed: () -> Unit,
    onWebAuthorizationUrlLoaded: (String) -> Unit
) {
    Scaffold(
        topBar = {
            ToolbarWithHelpButton(
                title = stringResource(id = R.string.log_in),
                onNavigationButtonClick = onBackClick,
                onHelpButtonClick = onHelpButtonClick,
                navigationIcon = if (viewState is LoginSiteCredentialsViewModel.ViewState.WebAuthorizationViewState) {
                    Icons.Filled.Clear
                } else {
                    Icons.AutoMirrored.Filled.ArrowBack
                }
            )
        }
    ) { paddingValues ->
        when (viewState) {
            is LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState -> NativeLoginForm(
                viewState = viewState,
                onUsernameChanged = onUsernameChanged,
                onPasswordChanged = onPasswordChanged,
                onContinueClick = onContinueClick,
                onResetPasswordClick = onResetPasswordClick,
                onErrorDialogDismissed = onErrorDialogDismissed,
                onHelpButtonClick = onHelpButtonClick,
                modifier = Modifier.padding(paddingValues)
            )

            is LoginSiteCredentialsViewModel.ViewState.WebAuthorizationViewState -> WebAuthorizationScreen(
                viewState = viewState,
                onPageFinished = onWebAuthorizationUrlLoaded,
                onErrorDialogDismissed = {
                    onErrorDialogDismissed()
                    onBackClick()
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun NativeLoginForm(
    viewState: LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onContinueClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onErrorDialogDismissed: () -> Unit,
    onHelpButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(id = R.dimen.major_100)),
        ) {
            Text(
                text = stringResource(id = R.string.enter_credentials_for_site, viewState.siteUrl),
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            WCOutlinedTextField(
                value = viewState.username,
                onValueChange = onUsernameChanged,
                label = stringResource(id = R.string.username),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            WCPasswordField(
                value = viewState.password,
                onValueChange = onPasswordChanged,
                label = stringResource(id = R.string.password),
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
                text = stringResource(id = R.string.continue_button)
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }

    if (viewState.errorDialogMessage != null) {
        AlertDialog(
            text = {
                Text(text = viewState.errorDialogMessage.getText())
            },
            onDismissRequest = onErrorDialogDismissed,
            buttons = {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                ) {
                    WCTextButton(
                        onClick = {
                            onErrorDialogDismissed()
                            onHelpButtonClick()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.login_site_address_more_help))
                    }
                    WCTextButton(
                        onClick = onErrorDialogDismissed
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        )
    }

    if (viewState.loadingMessage != null) {
        ProgressDialog(title = "", subtitle = stringResource(id = viewState.loadingMessage))
    }
}

@Composable
private fun WebAuthorizationScreen(
    viewState: LoginSiteCredentialsViewModel.ViewState.WebAuthorizationViewState,
    onPageFinished: (String) -> Unit,
    onErrorDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        viewState.loadingMessage != null -> {
            ProgressDialog(title = "", subtitle = stringResource(id = viewState.loadingMessage))
        }

        viewState.errorDialogMessage != null -> {
            AlertDialog(
                text = {
                    Text(text = viewState.errorDialogMessage.getText())
                },
                onDismissRequest = onErrorDialogDismissed,
                confirmButton = {
                    WCTextButton(
                        onClick = onErrorDialogDismissed
                    ) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            )
        }

        viewState.authorizationUrl != null -> {
            WCWebView(
                url = viewState.authorizationUrl,
                userAgent = viewState.userAgent,
                onPageFinished = onPageFinished,
                modifier = modifier
            )
        }
    }
}

@Preview
@Composable
private fun NativeLoginFormPreview() {
    WooThemeWithBackground {
        NativeLoginForm(
            viewState = LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState(
                siteUrl = "https://wordpress.com"
            ),
            onUsernameChanged = {},
            onPasswordChanged = {},
            onContinueClick = {},
            onResetPasswordClick = {},
            onErrorDialogDismissed = {},
            onHelpButtonClick = {}
        )
    }
}

@Preview
@Composable
private fun NativeLoginFormWithErrorDialogPreview() {
    WooThemeWithBackground {
        NativeLoginForm(
            viewState = LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState(
                siteUrl = "https://wordpress.com",
                errorDialogMessage = UiString.UiStringRes(R.string.login_site_credentials_fetching_site_failed)
            ),
            onUsernameChanged = {},
            onPasswordChanged = {},
            onContinueClick = {},
            onResetPasswordClick = {},
            onErrorDialogDismissed = {},
            onHelpButtonClick = {}
        )
    }
}
