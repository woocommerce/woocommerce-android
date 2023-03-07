package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.login.jetpack.components.UserInfo

@Composable
fun JetpackActivationMagicLinkRequestScreen(viewModel: JetpackActivationMagicLinkRequestViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationMagicLinkRequestScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick,
            onRequestMagicLinkClick = viewModel::onRequestMagicLinkClick,
            onOpenEmailClientClick = viewModel::onOpenEmailClientClick,
            onUsePasswordClick = viewModel::onUsePasswordClick
        )
    }
}

@Composable
fun JetpackActivationMagicLinkRequestScreen(
    viewState: JetpackActivationMagicLinkRequestViewModel.ViewState,
    onCloseClick: () -> Unit = {},
    onRequestMagicLinkClick: () -> Unit = {},
    onOpenEmailClientClick: () -> Unit = {},
    onUsePasswordClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = Filled.Clear
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(id = R.dimen.major_100))
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

            when (viewState) {
                is JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkRequestState -> {
                    MagicLinkRequestContent(viewState, onRequestMagicLinkClick, Modifier.weight(1f))
                }

                is JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkSentState -> {
                    MagicLinkSentContent(viewState, onOpenEmailClientClick, Modifier.weight(1f))
                }
            }
            if (viewState.allowPasswordLogin) {
                WCOutlinedButton(
                    onClick = onUsePasswordClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.enter_your_password_instead))
                }
            }
        }
    }
}

@Composable
private fun MagicLinkRequestContent(
    viewState: JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkRequestState,
    onRequestMagicLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = modifier
    ) {
        UserInfo(
            emailOrUsername = viewState.emailOrUsername,
            avatarUrl = viewState.avatarUrl,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = stringResource(id = R.string.login_magic_links_label))

        Spacer(modifier = Modifier.weight(1f))

        WCColoredButton(
            onClick = onRequestMagicLinkClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.send_link_by_email))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.login_magic_link_email_requesting))
    }
}

@Composable
private fun MagicLinkSentContent(
    viewState: JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkSentState,
    onOpenEmailClientClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    viewState.toString()
    Column(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(
                    id = org.wordpress.android.login.R.drawable.img_envelope
                ),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = stringResource(id = R.string.login_magic_links_sent_label_short),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            if (viewState.email != null) {
                Text(
                    text = stringResource(id = R.string.login_magic_links_email_sent),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = viewState.email,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(id = R.string.login_magic_links_email_sent_to_unknown_email),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        WCColoredButton(
            onClick = onOpenEmailClientClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.open_mail))
        }
    }
}

@Preview
@Composable
private fun MagicLinkRequestPreview() {
    WooThemeWithBackground {
        JetpackActivationMagicLinkRequestScreen(
            viewState = JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkRequestState(
                emailOrUsername = "test@email.com",
                avatarUrl = "avatar",
                isJetpackInstalled = false,
                allowPasswordLogin = true,
                isLoadingDialogShown = false
            )
        )
    }
}

@Preview
@Composable
private fun MagicLinkSentPreview() {
    WooThemeWithBackground {
        JetpackActivationMagicLinkRequestScreen(
            viewState = JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkSentState(
                email = null,
                isJetpackInstalled = false,
                allowPasswordLogin = true,
            )
        )
    }
}
