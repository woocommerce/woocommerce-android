package com.woocommerce.android.ui.login.jetpack.sitecredentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.JetpackActivationSiteCredentialsViewState

@Composable
fun JetpackActivationSiteCredentialsScreen(viewModel: JetpackActivationSiteCredentialsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationSiteCredentialsScreen(
            viewState = it,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onContinueClick = viewModel::onContinueClick
        )
    }
}

@Composable
fun JetpackActivationSiteCredentialsScreen(
    viewState: JetpackActivationSiteCredentialsViewState,
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize(),
    ) {
        Toolbar(onCloseClick = {})
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        ) {
            JetpackToWoo()
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = annotatedStringRes(
                    stringResId = if (viewState.isJetpackInstalled) R.string.login_jetpack_connection_enter_site_credentials
                    else R.string.login_jetpack_installation_enter_site_credentials,
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
                helperText = viewState.errorMessage?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onContinueClick() }
                )
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colorResource(id = R.color.color_on_surface_medium),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_40))
                )
                Text(
                    text = stringResource(id = R.string.login_jetpack_connection_approval_hint),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = R.color.color_on_surface_medium)
                )
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
                    id = if (viewState.isJetpackInstalled) R.string.login_jetpack_connect
                    else R.string.login_jetpack_install
                )
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }

    if (viewState.isLoading) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.logging_in))
    }
}

@Composable
private fun JetpackToWoo(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val logoModifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_100))
        Image(
            painter = painterResource(id = R.drawable.ic_jetpack_logo),
            contentDescription = null,
            modifier = logoModifier
        )
        Image(painter = painterResource(id = R.drawable.ic_connecting), contentDescription = null)
        Image(
            painter = painterResource(id = R.drawable.ic_woo_bubble),
            contentDescription = null,
            modifier = logoModifier
        )
    }
}

@Composable
private fun Toolbar(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = { Text(stringResource(id = R.string.login_jetpack_installation_screen_title)) },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
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
