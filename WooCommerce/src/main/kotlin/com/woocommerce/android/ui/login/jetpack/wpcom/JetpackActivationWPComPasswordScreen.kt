package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader

@Composable
fun JetpackActivationWPComPasswordScreen(viewModel: JetpackActivationWPComPasswordViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationWPComPasswordScreen(
            viewState = it,
            onPasswordChanged = viewModel::onPasswordChanged,
            onCloseClick = viewModel::onCloseClick,
            onContinueClick = viewModel::onContinueClick,
            onMagicLinkClick = viewModel::onMagicLinkClick,
            onResetPasswordClick = viewModel::onResetPasswordClick
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JetpackActivationWPComPasswordScreen(
    viewState: JetpackActivationWPComPasswordViewModel.ViewState,
    onPasswordChanged: (String) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onMagicLinkClick: () -> Unit = {},
    onResetPasswordClick: () -> Unit = {}
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
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                UserInfo(
                    emailOrUsername = viewState.emailOrUsername,
                    avatarUrl = viewState.avatarUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(id = R.string.enter_wpcom_password)
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
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
            WCOutlinedButton(
                onClick = onMagicLinkClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(id = R.string.login_jetpack_installation_continue_magic_link)
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.logging_in))
    }
}

@Composable
private fun UserInfo(
    emailOrUsername: String,
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.major_100),
            Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, color = colorResource(id = R.color.divider_color), shape = MaterialTheme.shapes.medium)
            .semantics(mergeDescendants = true) {}
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        AsyncImage(
            model = Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .placeholder(R.drawable.img_gravatar_placeholder)
                .error(R.drawable.img_gravatar_placeholder)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.image_minor_100))
                .clip(CircleShape)
        )
        Text(
            text = emailOrUsername,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Preview
@Composable
private fun JetpackActivationWPComScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationWPComPasswordScreen(
            viewState = JetpackActivationWPComPasswordViewModel.ViewState(
                emailOrUsername = "test@email.com",
                password = "",
                avatarUrl = "",
                isJetpackInstalled = false
            )
        )
    }
}
