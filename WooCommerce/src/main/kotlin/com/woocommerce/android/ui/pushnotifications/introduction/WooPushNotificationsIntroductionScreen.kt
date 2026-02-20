package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge
import com.woocommerce.android.ui.pushnotifications.introduction.WooPushNotificationsIntroductionViewModel.ErrorType
import com.woocommerce.android.ui.pushnotifications.introduction.WooPushNotificationsIntroductionViewModel.ViewState

@Composable
fun WooPushNotificationsIntroductionScreen(viewModel: WooPushNotificationsIntroductionViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        WooPushNotificationsIntroductionScreen(
            viewState = viewState,
            onContinueClick = viewModel::onContinueClick,
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
    onNotNowClick: () -> Unit,
    onWhatIsWPComClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val errorType = viewState.errorType
        if (errorType != null) {
            ErrorContent(
                errorType = errorType,
                onContactSupportClick = onContactSupportClick,
                onNotNowClick = onNotNowClick,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            IntroContent(
                viewState = viewState,
                onContinueClick = onContinueClick,
                onNotNowClick = onNotNowClick,
                onWhatIsWPComClick = onWhatIsWPComClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IntroContent(
    viewState: ViewState,
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
                text = stringResource(id = R.string.woo_push_notifications_introduction_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_body),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

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

        WCColoredButton(
            onClick = onContinueClick,
            enabled = !viewState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(id = R.string.woo_push_notifications_introduction_continue))
            }
        }

        WCOutlinedButton(
            onClick = onNotNowClick,
            enabled = !viewState.isLoading,
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
    errorType: ErrorType,
    onContactSupportClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyText = when (errorType) {
        ErrorType.Generic -> stringResource(
            id = R.string.woo_push_notifications_introduction_error_body
        )
        ErrorType.Forbidden -> stringResource(
            id = R.string.woo_push_notifications_introduction_error_forbidden_body
        )
    }

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
            Text(text = stringResource(id = R.string.help))
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
private fun WooPushNotificationsIntroductionScreenPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState(),
            onContinueClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionScreenLoadingPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState(isLoading = true),
            onContinueClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionScreenErrorPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState(errorType = ErrorType.Generic),
            onContinueClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}

@Composable
@Preview
private fun WooPushNotificationsIntroductionScreenForbiddenErrorPreview() {
    WooThemeWithBackground {
        WooPushNotificationsIntroductionScreen(
            viewState = ViewState(errorType = ErrorType.Forbidden),
            onContinueClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {},
            onContactSupportClick = {}
        )
    }
}
