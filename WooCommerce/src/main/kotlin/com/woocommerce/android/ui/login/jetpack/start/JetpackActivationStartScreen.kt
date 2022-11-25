package com.woocommerce.android.ui.login.jetpack.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.start.JetpackActivationStartViewModel.JetpackActivationState
import com.woocommerce.android.util.ChromeCustomTabUtils

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
            Toolbar(
                isConnectionDismissed = viewState.isConnectionDismissed,
                onHelpButtonClick = onHelpButtonClick,
                onBackButtonClick = onBackButtonClick
            )
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
                    false -> StartState(viewState.isJetpackInstalled, viewState.url)
                    true -> ConnectionDismissedState()
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
                    Text(stringResource(id = R.string.login_jetpack_installation_cancel))
                }
            } else {
                JetpackConsent(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.StartState(isJetpackInstalled: Boolean, siteUrl: String) {
    Image(
        painter = painterResource(
            id = if (isJetpackInstalled) R.drawable.img_connect_jetpack
            else R.drawable.img_install_jetpack
        ),
        contentDescription = null,
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .weight(1f, false)
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    Text(
        text = annotatedStringRes(
            stringResId = if (isJetpackInstalled) R.string.login_jetpack_connection_explanation
            else R.string.login_jetpack_installation_explanation,
            siteUrl
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
private fun ColumnScope.ConnectionDismissedState() {
    Image(
        painter = painterResource(
            id = R.drawable.img_jetpack_connection_dismissed
        ),
        contentDescription = null,
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .weight(1f, false)
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    Text(
        text = stringResource(id = R.string.login_jetpack_installation_connection_dismissed),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = stringResource(id = R.string.login_jetpack_installation_connection_dismissed_explanation),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Toolbar(
    isConnectionDismissed: Boolean,
    onHelpButtonClick: () -> Unit,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {
            if (!isConnectionDismissed) {
                Text(stringResource(id = R.string.login_jetpack_installation_screen_title))
            }
        },
        navigationIcon = {
            if (!isConnectionDismissed) {
                IconButton(onClick = onBackButtonClick) {
                    Icon(
                        Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onHelpButtonClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_help_24dp),
                    contentDescription = stringResource(id = R.string.help)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun JetpackConsent(modifier: Modifier = Modifier) {
    val consent = annotatedStringRes(stringResId = R.string.login_jetpack_connection_consent)
    val context = LocalContext.current
    ClickableText(
        text = consent,
        style = MaterialTheme.typography.caption.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface
        ),
        modifier = modifier
    ) {
        consent.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
            .firstOrNull()
            ?.let { annotation ->
                when (annotation.item) {
                    "terms" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.WORPRESS_COM_TERMS)
                    "sync" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.JETPACK_SYNC_POLICY)
                }
            }
    }
}

@Composable
@Preview
private fun JetpackActivationStartPreview() {
    WooThemeWithBackground {
        JetpackActivationStartScreen(
            viewState = JetpackActivationState(
                url = "reallyniceshirts.com",
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
                isJetpackInstalled = false,
                isConnectionDismissed = true
            )
        )
    }
}
