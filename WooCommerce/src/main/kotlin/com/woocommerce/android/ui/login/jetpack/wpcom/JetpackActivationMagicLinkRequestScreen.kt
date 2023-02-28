package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.UserInfo

@Composable
fun JetpackActivationMagicLinkRequestScreen(viewModel: JetpackActivationMagicLinkRequestViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationMagicLinkRequestScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick
        )
    }
}

@Composable
fun JetpackActivationMagicLinkRequestScreen(
    viewState: JetpackActivationMagicLinkRequestViewModel.ViewState,
    onCloseClick: () -> Unit = {}
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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)

        when (viewState) {
            is JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkRequestState -> {
                MagicLinkRequestContent(viewState, contentModifier)
            }

            is JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkSentState -> {
                TODO()
            }
        }
    }
}

@Composable
private fun MagicLinkRequestContent(
    viewState: JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkRequestState,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = modifier.padding(dimensionResource(id = R.dimen.major_100))
    ) {
        UserInfo(
            emailOrUsername = viewState.email,
            avatarUrl = viewState.avatarUrl,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = stringResource(id = R.string.login_magic_links_label))

        Spacer(modifier = Modifier.weight(1f))

        WCColoredButton(
            onClick = { TODO() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.send_link_by_email))
        }
    }

    if (viewState.isLoadingDialogShown) {
        ProgressDialog(title = "", subtitle = stringResource(id = R.string.login_magic_link_email_requesting))
    }
}

@Preview
@Composable
private fun MagicLinkRequestPreview() {
    WooThemeWithBackground {
        JetpackActivationMagicLinkRequestScreen(
            viewState = JetpackActivationMagicLinkRequestViewModel.ViewState.MagicLinkRequestState(
                email = "test@email.com",
                avatarUrl = "avatar",
                isLoadingDialogShown = false
            )
        )
    }
}
