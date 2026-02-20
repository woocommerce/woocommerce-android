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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge

@Composable
fun WooPushNotificationsIntroductionScreen(
    onContinueClick: () -> Unit,
    onNotNowClick: () -> Unit,
    onWhatIsWPComClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Content centered in remaining space
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

            // Title
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            // Body
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_body),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            // Body 2
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_body2),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 24.dp)
            )

            // What is WordPress.com? link
            Text(
                text = stringResource(id = R.string.woo_push_notifications_introduction_what_is_wpcom),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(id = R.color.color_primary),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clickable { onWhatIsWPComClick() }
            )
        }

        // Continue button
        WCColoredButton(
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.woo_push_notifications_introduction_continue))
        }

        // Not now button
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
            onContinueClick = {},
            onNotNowClick = {},
            onWhatIsWPComClick = {}
        )
    }
}
