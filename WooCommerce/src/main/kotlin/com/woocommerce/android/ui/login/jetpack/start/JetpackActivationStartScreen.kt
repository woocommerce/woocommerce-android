package com.woocommerce.android.ui.login.jetpack.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackConsent
import com.woocommerce.android.ui.login.jetpack.start.JetpackActivationStartViewModel.JetpackActivationState

@Composable
fun JetpackActivationStartScreen(viewModel: JetpackActivationStartViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationStartScreen(
            it,
            onContinueButtonClick = viewModel::onContinueButtonClick,
            onHelpButtonClick = viewModel::onHelpButtonClick,
            onBackButtonClick = viewModel::onBackButtonClick
        )
    }
}

@Composable
fun JetpackActivationStartScreen(
    viewState: JetpackActivationState,
    onContinueButtonClick: () -> Unit = {},
    onHelpButtonClick: () -> Unit = {},
    onBackButtonClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            if (!viewState.isConnectionDismissed) {
                ToolbarWithHelpButton(
                    title = stringResource(id = string.login_jetpack_installation_screen_title),
                    onNavigationButtonClick = onBackButtonClick,
                    navigationIcon = Icons.Filled.Clear,
                    onHelpButtonClick = onHelpButtonClick,
                )
            } else {
                Toolbar(
                    title = { Text("") },
                    actions = {
                        IconButton(onClick = onHelpButtonClick) {
                            Icon(
                                painter = painterResource(id = drawable.ic_help_24dp),
                                contentDescription = stringResource(id = string.help)
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.surface)
                .padding(paddingValues)
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                when (viewState.isConnectionDismissed) {
                    false -> StartState(viewState.isJetpackInstalled, viewState.url, viewState.faviconUrl)
                    true -> ConnectionDismissedState(viewState.url, viewState.faviconUrl)
                }
            }
            Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
            WCColoredButton(
                onClick = onContinueButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(
                        id = when {
                            viewState.isConnectionDismissed -> R.string.login_jetpack_installation_continue_connection
                            viewState.isJetpackInstalled -> R.string.login_jetpack_connect
                            else -> R.string.login_jetpack_install
                        }
                    )
                )
            }
            if (viewState.isConnectionDismissed) {
                WCOutlinedButton(
                    onClick = onBackButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                ) {
                    Text(stringResource(id = R.string.login_jetpack_installation_exit_without_connection))
                }
            } else {
                JetpackConsent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.StartState(
    isJetpackInstalled: Boolean,
    siteUrl: String,
    faviconUrl: String
) {
    Image(
        painter = painterResource(
            id = if (isJetpackInstalled) {
                R.drawable.img_connect_jetpack
            } else {
                R.drawable.img_install_jetpack
            }
        ),
        contentDescription = null,
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .weight(1f, false)
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
    SiteUrlAndIcon(siteUrl = siteUrl, faviconUrl = faviconUrl, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
    Text(
        text = stringResource(
            id = if (isJetpackInstalled) {
                R.string.login_jetpack_connection_explanation
            } else {
                R.string.login_jetpack_installation_explanation
            }
        ),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    Text(
        text = stringResource(id = R.string.login_jetpack_installation_credentials_hint),
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.color_on_surface_medium),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ColumnScope.ConnectionDismissedState(
    siteUrl: String,
    faviconUrl: String
) {
    Image(
        painter = painterResource(
            id = R.drawable.img_jetpack_connection_dismissed
        ),
        contentDescription = null,
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .weight(1f, false)
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
    SiteUrlAndIcon(siteUrl = siteUrl, faviconUrl = faviconUrl, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
    Text(
        text = stringResource(id = R.string.login_jetpack_installation_connection_dismissed),
        style = MaterialTheme.typography.h6,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
    Text(
        text = stringResource(id = R.string.login_jetpack_installation_connection_dismissed_explanation),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SiteUrlAndIcon(
    siteUrl: String,
    faviconUrl: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.major_100),
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, color = colorResource(id = R.color.divider_color), shape = MaterialTheme.shapes.medium)
            .semantics(mergeDescendants = true) {}
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(faviconUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_globe)
                .error(R.drawable.ic_globe)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_40))
        )
        Text(
            text = siteUrl,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
@Preview
private fun JetpackActivationStartPreview() {
    WooThemeWithBackground {
        JetpackActivationStartScreen(
            viewState = JetpackActivationState(
                url = "reallyniceshirts.com",
                faviconUrl = "wordpress.com/favicon.ico",
                isJetpackInstalled = false,
                isConnectionDismissed = false
            )
        )
    }
}

@Composable
@Preview
private fun JetpackConnectionDismissPreview() {
    WooThemeWithBackground {
        JetpackActivationStartScreen(
            viewState = JetpackActivationState(
                url = "reallyniceshirts.com",
                faviconUrl = "",
                isJetpackInstalled = false,
                isConnectionDismissed = true
            )
        )
    }
}
