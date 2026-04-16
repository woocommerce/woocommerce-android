package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.wpcom.components.WPComConsent
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge
import com.woocommerce.android.ui.pushnotifications.introduction.WooPushNotificationsIntroductionViewModel.ViewState

@Composable
fun WooPushNotificationsIntroductionScreen(viewModel: WooPushNotificationsIntroductionViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        WooPushNotificationsIntroductionScreen(
            viewState = viewState,
            onContinueClick = viewModel::onContinueClick,
            onCloseClick = viewModel::onCloseClick,
            onNotNowClick = viewModel::onNotNowClick,
            onWhatIsWPComClick = viewModel::onWhatIsWPComClick,
            onContactSupportClick = viewModel::onContactSupportClick
        )
    }
}

@Composable
fun WooPushNotificationsIntroductionScreen(
    viewState: ViewState,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onNotNowClick: () -> Unit,
    onWhatIsWPComClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (viewState) {
                ViewState.Loading -> LoadingContent(modifier = Modifier.fillMaxWidth())
                ViewState.NotConnected, ViewState.UpdateRequired -> IntroContent(
                    isUpdateRequired = viewState is ViewState.UpdateRequired,
                    onContinueClick = onContinueClick,
                    onNotNowClick = onNotNowClick,
                    onWhatIsWPComClick = onWhatIsWPComClick,
                    modifier = Modifier.fillMaxWidth()
                )

                ViewState.Connected -> ConnectedContent(
                    onContinueClick = onContinueClick,
                    onNotNowClick = onNotNowClick,
                    modifier = Modifier.fillMaxWidth()
                )

                ViewState.ForbiddenError -> ErrorContent(
                    bodyText = stringResource(
                        id = R.string.woo_push_notifications_introduction_error_forbidden_body
                    ),
                    onContactSupportClick = onContactSupportClick,
                    onNotNowClick = onNotNowClick,
                    modifier = Modifier.fillMaxWidth()
                )

                ViewState.GenericError -> ErrorContent(
                    bodyText = stringResource(
                        id = R.string.woo_push_notifications_introduction_error_body
                    ),
                    onContactSupportClick = onContactSupportClick,
                    onNotNowClick = onNotNowClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            SkeletonView(
                modifier = Modifier
                    .width(100.dp)
                    .height(64.dp)
            )

            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(top = 24.dp)
            )

            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 24.dp)
            )

            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 24.dp)
            )

            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .padding(top = 24.dp)
            )
        }

        SkeletonView(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        SkeletonView(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun IntroContent(
    isUpdateRequired: Boolean,
    onContinueClick: () -> Unit,
    onNotNowClick: () -> Unit,
    onWhatIsWPComClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            WordPressWooBadge(
                iconSize = 64.dp
            )

            Text(
                text = stringResource(
                    id = if (isUpdateRequired) {
                        R.string.woo_push_notifications_introduction_update_required_title
                    } else {
                        R.string.woo_push_notifications_introduction_title
                    }
                ),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = stringResource(
                    id = if (isUpdateRequired) {
                        R.string.woo_push_notifications_introduction_update_required_body
                    } else {
                        R.string.woo_push_notifications_introduction_body
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            if (!isUpdateRequired) {
                Text(
                    text = stringResource(id = R.string.woo_push_notifications_introduction_body2),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Text(
                    text = stringResource(id = R.string.woo_push_notifications_introduction_what_is_wpcom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorResource(id = R.color.color_primary),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .clickable { onWhatIsWPComClick() }
                )
            }
        }

        WCColoredButton(
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    if (isUpdateRequired) {
                        R.string.woo_push_notifications_introduction_update_plugin
                    } else {
                        R.string.woo_push_notifications_introduction_continue
                    }
                )
            )
        }

        WCOutlinedButton(
            onClick = onNotNowClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_not_now),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (!isUpdateRequired) {
            WPComConsent(
                forJetpackSetup = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ConnectedContent(
    onContinueClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            WordPressWooBadge(
                iconSize = 64.dp
            )

            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_connected_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_connected_body),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        WCColoredButton(
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.woo_push_notifications_introduction_continue))
        }

        WCOutlinedButton(
            onClick = onNotNowClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_not_now),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorContent(
    bodyText: String,
    onContactSupportClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gridicons_notice),
                contentDescription = null,
                tint = colorResource(id = R.color.color_error),
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_error_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = bodyText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        WCColoredButton(
            onClick = onContactSupportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.support_contact))
        }

        WCOutlinedButton(
            onClick = onNotNowClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_not_now),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionLoadingPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState.Loading,
            onContinueClick = {},
            onCloseClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionNotConnectedPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState.NotConnected,
            onContinueClick = {},
            onCloseClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionUpdateRequiredPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState.UpdateRequired,
            onContinueClick = {},
            onCloseClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionConnectedPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState.Connected,
            onContinueClick = {},
            onCloseClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionErrorPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState.GenericError,
            onContinueClick = {},
            onCloseClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}
