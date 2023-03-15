package com.woocommerce.android.ui.login.sitecredentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.getText

@Composable
fun LoginSiteCredentialsScreen(viewModel: LoginSiteCredentialsViewModel) {
    viewModel.state.observeAsState().value?.let {
        LoginSiteCredentialsScreen(
            viewState = it,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onContinueClick = viewModel::onContinueClick,
            onResetPasswordClick = viewModel::onResetPasswordClick,
            onBackClick = viewModel::onBackClick,
            onHelpButtonClick = viewModel::onHelpButtonClick,
            onErrorDialogDismissed = viewModel::onErrorDialogDismissed
        )
    }
}

@Composable
fun LoginSiteCredentialsScreen(
    viewState: LoginSiteCredentialsViewModel.LoginSiteCredentialsViewState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onContinueClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onBackClick: () -> Unit,
    onHelpButtonClick: () -> Unit,
    onErrorDialogDismissed: () -> Unit
) {
    Scaffold(
        topBar = {
            ToolbarWithHelpButton(
                title = stringResource(id = R.string.log_in),
                onNavigationButtonClick = onBackClick,
                onHelpButtonClick = onHelpButtonClick
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
    }

    if (viewState.errorDialogMessage != null) {
        AlertDialog(
            text = {
                Text(text = viewState.errorDialogMessage.getText())
            },
            onDismissRequest = onErrorDialogDismissed,
            buttons = {
                Row(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
                    WCTextButton(onClick = onHelpButtonClick) {
                        Text(text = stringResource(id = R.string.login_site_address_more_help))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    WCTextButton(
                        onClick = onErrorDialogDismissed
                    ) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            }
        )
    }

    if (viewState.isLoading) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.logging_in))
    }
}
