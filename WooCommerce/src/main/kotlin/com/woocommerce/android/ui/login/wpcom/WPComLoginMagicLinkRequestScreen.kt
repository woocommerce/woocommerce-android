package com.woocommerce.android.ui.login.wpcom

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader
import com.woocommerce.android.ui.login.wpcom.components.UserInfo
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge
import org.wordpress.android.login.MagicLinkFallbackButton

@Composable
fun WPComLoginMagicLinkRequestScreen(viewModel: WPComLoginMagicLinkRequestViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        WPComLoginMagicLinkRequestScreen(
            viewState = it,
            onCloseClick = viewModel::onCloseClick,
            onRequestMagicLinkClick = viewModel::onRequestMagicLinkClick,
            onOpenEmailClientClick = viewModel::onOpenEmailClientClick,
            onFallbackButtonClick = viewModel::onFallbackButtonClick
        )
    }
}

@Composable
fun WPComLoginMagicLinkRequestScreen(
    viewState: WPComLoginMagicLinkRequestViewModel.ViewState,
    onCloseClick: () -> Unit = {},
    onRequestMagicLinkClick: () -> Unit = {},
    onOpenEmailClientClick: () -> Unit = {},
    onFallbackButtonClick: () -> Unit = {}
) {
    val branding = viewState.resolveBranding()

    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = ImageVector.vectorResource(branding.navIcon)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (viewState.wpComLoginMode is WPComLoginMode.PushNotificationsSetup) {
                WordPressWooBadge()
            } else {
                JetpackToWooHeader()
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(id = branding.title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (viewState) {
                is WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkRequestState -> {
                    MagicLinkRequestContent(viewState, onRequestMagicLinkClick, Modifier.weight(1f))
                }

                is WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkSentState -> {
                    MagicLinkSentContent(viewState, onOpenEmailClientClick, Modifier.weight(1f))
                }
            }

            val fallbackButtonText = when (viewState.magicLinkFallbackButton) {
                MagicLinkFallbackButton.Password -> R.string.enter_your_password_instead
                MagicLinkFallbackButton.UsernameAndPassword -> R.string.login_use_wpcom_username_instead
                MagicLinkFallbackButton.None -> null
            }
            fallbackButtonText?.let {
                WCOutlinedButton(
                    onClick = onFallbackButtonClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = it))
                }
            }
        }
    }
}

@Composable
private fun MagicLinkRequestContent(
    viewState: WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkRequestState,
    onRequestMagicLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
    viewState: WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkSentState,
    onOpenEmailClientClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.login_magic_links_sent_label_short),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (viewState.email != null) {
                Text(
                    text = stringResource(id = R.string.login_magic_links_email_sent),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewState.email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.login_magic_links_email_sent_double_check_email),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(id = R.string.login_magic_links_email_sent_to_unknown_email),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        WCColoredButton(
            onClick = onOpenEmailClientClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.open_mail))
        }
    }
}

private data class MagicLinkScreenBranding(
    @DrawableRes val navIcon: Int,
    @StringRes val title: Int,
)

private fun WPComLoginMagicLinkRequestViewModel.ViewState.resolveBranding(): MagicLinkScreenBranding {
    return when (wpComLoginMode) {
        WPComLoginMode.PushNotificationsSetup -> MagicLinkScreenBranding(
            navIcon = R.drawable.ic_back_24dp,
            title = R.string.login_wpcom_connect_title,
        )

        is WPComLoginMode.JetpackSetup -> MagicLinkScreenBranding(
            navIcon = R.drawable.ic_close_24dp,
            title = if (isJetpackInstalled) {
                R.string.login_jetpack_connect
            } else {
                R.string.login_jetpack_install
            },
        )
    }
}

@Preview
@Composable
private fun JetpackModeRequestPreview() {
    WooThemeWithBackground {
        WPComLoginMagicLinkRequestScreen(
            viewState = WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkRequestState(
                wpComLoginMode = WPComLoginMode.JetpackSetup(
                    com.woocommerce.android.model.JetpackStatus(
                        isJetpackInstalled = false,
                        jetpackConnectionStatus = com.woocommerce.android.model.JetpackConnectionStatus
                            .AccountNotConnected(
                                siteRegistrationStatus = com.woocommerce.android.model
                                    .JetpackSiteRegistrationStatus.UNKNOWN,
                                blogId = null
                            )
                    )
                ),
                emailOrUsername = "test@email.com",
                avatarUrl = "avatar",
                isJetpackInstalled = false,
                magicLinkFallbackButton = MagicLinkFallbackButton.Password,
                isLoadingDialogShown = false
            )
        )
    }
}

@Preview
@Composable
private fun NotificationSetupModeRequestPreview() {
    WooThemeWithBackground {
        WPComLoginMagicLinkRequestScreen(
            viewState = WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkRequestState(
                wpComLoginMode = WPComLoginMode.PushNotificationsSetup,
                emailOrUsername = "test@email.com",
                avatarUrl = "avatar",
                isJetpackInstalled = false,
                magicLinkFallbackButton = MagicLinkFallbackButton.Password,
                isLoadingDialogShown = false
            )
        )
    }
}

@Preview
@Composable
private fun MagicLinkSentPreview() {
    WooThemeWithBackground {
        WPComLoginMagicLinkRequestScreen(
            viewState = WPComLoginMagicLinkRequestViewModel.ViewState.MagicLinkSentState(
                wpComLoginMode = WPComLoginMode.PushNotificationsSetup,
                email = null,
                isJetpackInstalled = false,
                magicLinkFallbackButton = MagicLinkFallbackButton.Password,
            )
        )
    }
}
