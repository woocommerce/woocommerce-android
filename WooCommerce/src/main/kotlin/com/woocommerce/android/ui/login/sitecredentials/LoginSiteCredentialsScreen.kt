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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun LoginSiteCredentialsScreen(viewModel: LoginSiteCredentialsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        LoginSiteCredentialsScreen(
            viewState = it,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onEndpointUrlChanged = viewModel::onEndpointUrlChanged,
            onContinueClick = viewModel::onContinueClick,
            onEndpointRecoveryCancelClick = viewModel::onEndpointRecoveryCancelClick,
            onResetPasswordClick = viewModel::onResetPasswordClick,
            onBackClick = viewModel::onBackClick,
            onHelpButtonClick = viewModel::onHelpButtonClick,
            onErrorDialogDismissed = viewModel::onErrorDialogDismissed,
            onStartWebAuthorizationClick = viewModel::onStartWebAuthorizationClick
        )
    }
}

@Composable
fun LoginSiteCredentialsScreen(
    viewState: LoginSiteCredentialsViewModel.ViewState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEndpointUrlChanged: (String) -> Unit,
    onContinueClick: () -> Unit,
    onEndpointRecoveryCancelClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onBackClick: () -> Unit,
    onHelpButtonClick: () -> Unit,
    onErrorDialogDismissed: () -> Unit,
    onStartWebAuthorizationClick: () -> Unit
) {
    Scaffold(
        topBar = {
            ToolbarWithHelpButton(
                title = stringResource(id = R.string.log_in),
                onNavigationButtonClick = onBackClick,
                onHelpButtonClick = onHelpButtonClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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
                if (viewState.endpointRecovery != null) {
                    EndpointRecoveryForm(
                        recovery = viewState.endpointRecovery,
                        onUrlChanged = onEndpointUrlChanged,
                        onContinueClick = onContinueClick
                    )
                } else {
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
                        keyboardActions = KeyboardActions(onDone = { onContinueClick() })
                    )
                    WCTextButton(onClick = onResetPasswordClick) {
                        Text(text = stringResource(id = R.string.reset_your_password))
                    }
                }
            }

            WCColoredButton(
                onClick = onContinueClick,
                enabled = viewState.endpointRecovery?.url?.isNotBlank() ?: viewState.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(id = R.string.continue_button)
                )
            }
            if (viewState.endpointRecovery != null) {
                WCTextButton(
                    onClick = onStartWebAuthorizationClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = stringResource(id = R.string.login_site_credentials_use_web_authorization))
                }
                WCTextButton(
                    onClick = onEndpointRecoveryCancelClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }

        if (viewState.authenticationError != null) {
            AlertDialog(
                text = {
                    Text(text = viewState.authenticationError.errorMessage.getText())
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
                        if (viewState.authenticationError.showWpAdminFallbackOption) {
                            WCTextButton(
                                onClick = onStartWebAuthorizationClick
                            ) {
                                Text(text = stringResource(id = R.string.login_site_credentials_use_web_authorization))
                            }
                        }
                    }
                }
            )
        }

        if (viewState.loadingMessage != null) {
            ProgressDialog(title = "", subtitle = stringResource(id = viewState.loadingMessage))
        }
    }
}

@Composable
private fun EndpointRecoveryForm(
    recovery: LoginSiteCredentialsViewModel.EndpointRecovery,
    onUrlChanged: (String) -> Unit,
    onContinueClick: () -> Unit
) {
    val strings = if (recovery.type == LoginSiteCredentialsViewModel.EndpointType.LOGIN) {
        Triple(
            R.string.login_site_credentials_login_url_title,
            R.string.login_site_credentials_login_url_description,
            R.string.login_site_credentials_login_url_label
        )
    } else {
        Triple(
            R.string.login_site_credentials_admin_url_title,
            R.string.login_site_credentials_admin_url_description,
            R.string.login_site_credentials_admin_url_label
        )
    }
    Text(
        text = stringResource(strings.first),
        style = MaterialTheme.typography.h6
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
    Text(
        text = stringResource(strings.second),
        style = MaterialTheme.typography.body2
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    WCOutlinedTextField(
        value = recovery.url,
        onValueChange = onUrlChanged,
        label = stringResource(strings.third),
        helperText = recovery.errorMessage?.getText(),
        isError = recovery.errorMessage != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onContinueClick() })
    )
}

@LightDarkThemePreviews
@Composable
private fun LoginSiteCredentialsScreenPreview() {
    LoginSiteCredentialsScreenPreview(
        LoginSiteCredentialsViewModel.ViewState(siteUrl = "https://wordpress.com")
    )
}

@LightDarkThemePreviews
@Composable
private fun LoginSiteCredentialsScreenWithErrorPreview() {
    LoginSiteCredentialsScreenPreview(
        LoginSiteCredentialsViewModel.ViewState(
            siteUrl = "https://wordpress.com",
            authenticationError = LoginSiteCredentialsViewModel.AuthenticationError(
                errorMessage = UiString.UiStringRes(R.string.login_site_credentials_fetching_site_failed),
                showWpAdminFallbackOption = true
            )
        )
    )
}

@LightDarkThemePreviews
@Composable
private fun LoginSiteCredentialsLoginRecoveryPreview() {
    LoginSiteCredentialsScreenPreview(
        LoginSiteCredentialsViewModel.ViewState(
            siteUrl = "example.com",
            endpointRecovery = LoginSiteCredentialsViewModel.EndpointRecovery(
                type = LoginSiteCredentialsViewModel.EndpointType.LOGIN,
                url = "https://example.com/wp-login.php"
            )
        )
    )
}

@LightDarkThemePreviews
@Composable
private fun LoginSiteCredentialsAdminRecoveryErrorPreview() {
    LoginSiteCredentialsScreenPreview(
        LoginSiteCredentialsViewModel.ViewState(
            siteUrl = "example.com",
            endpointRecovery = LoginSiteCredentialsViewModel.EndpointRecovery(
                type = LoginSiteCredentialsViewModel.EndpointType.ADMIN,
                url = "https://example.com/dashboard",
                errorMessage = UiString.UiStringRes(
                    R.string.login_site_credentials_admin_url_not_found_error
                )
            )
        )
    )
}

@Composable
private fun LoginSiteCredentialsScreenPreview(viewState: LoginSiteCredentialsViewModel.ViewState) {
    WooThemeWithBackground {
        LoginSiteCredentialsScreen(
            viewState = viewState,
            onUsernameChanged = {},
            onPasswordChanged = {},
            onEndpointUrlChanged = {},
            onContinueClick = {},
            onEndpointRecoveryCancelClick = {},
            onResetPasswordClick = {},
            onBackClick = {},
            onHelpButtonClick = {},
            onErrorDialogDismissed = {},
            onStartWebAuthorizationClick = {}
        )
    }
}
