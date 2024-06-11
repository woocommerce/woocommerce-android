package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun NotificationSettingsScreen(viewModel: NotificationSettingsViewModel) {
    viewModel.newOrderNotificationSoundStatus.observeAsState().value?.let {
        NotificationSettingsScreen(
            orderNotificationSoundStatus = it,
            onManageNotificationsClicked = viewModel::onManageNotificationsClicked,
            onEnableChaChingSoundClicked = viewModel::onEnableChaChingSoundClicked
        )
    }
}

@Composable
private fun NotificationSettingsScreen(
    orderNotificationSoundStatus: NewOrderNotificationSoundStatus,
    onManageNotificationsClicked: () -> Unit,
    onEnableChaChingSoundClicked: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
        ) {
            NotificationSettingsItem(
                title = stringResource(id = R.string.settings_notifs_device),
                subtitle = stringResource(id = R.string.settings_notifs_device_detail),
                onClick = onManageNotificationsClicked,
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = orderNotificationSoundStatus.requiresAttention) {
                val subtitle = if (orderNotificationSoundStatus == NewOrderNotificationSoundStatus.DISABLED) {
                    R.string.settings_notifs_enable_chaching_sound_description
                } else {
                    R.string.settings_notifs_restore_chaching_sound_description
                }
                NotificationSettingsItem(
                    title = stringResource(id = R.string.settings_notifs_enable_chaching_sound),
                    subtitle = stringResource(id = subtitle),
                    onClick = onEnableChaChingSoundClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NotificationSettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.body2
        )
    }
}

private val NewOrderNotificationSoundStatus.requiresAttention: Boolean
    get() = this != NewOrderNotificationSoundStatus.DEFAULT

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        NotificationSettingsScreen(
            orderNotificationSoundStatus = NewOrderNotificationSoundStatus.DISABLED,
            onManageNotificationsClicked = {},
            onEnableChaChingSoundClicked = {}
        )
    }
}
