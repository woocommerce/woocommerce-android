package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsViewModel.NotificationType
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsViewModel.NotificationTypeItem

@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    showSmarterNotifications: Boolean
) {
    if (showSmarterNotifications) {
        viewModel.notificationTypeItems.observeAsState().value?.let {
            val isAppNotificationsEnabled = viewModel.isAppNotificationsEnabled.observeAsState(initial = true).value

            SmarterNotificationSettingsScreen(
                items = it,
                isAppNotificationsEnabled = isAppNotificationsEnabled,
                onNotificationTypeEnabledChanged = viewModel::onNotificationTypeEnabledChanged,
                onNotificationTypeClicked = viewModel::onNotificationTypeClicked,
                onDeviceNotificationSettingsClicked = viewModel::onDeviceNotificationSettingsClicked
            )
        }
    } else {
        viewModel.newOrderNotificationSoundStatus.observeAsState().value?.let {
            NotificationSettingsScreen(
                orderNotificationSoundStatus = it,
                onDeviceNotificationSettingsClicked = viewModel::onDeviceNotificationSettingsClicked,
                onEnableChaChingSoundClicked = viewModel::onEnableChaChingSoundClicked
            )
        }
    }
}

@Composable
private fun SmarterNotificationSettingsScreen(
    items: List<NotificationTypeItem>,
    isAppNotificationsEnabled: Boolean,
    onNotificationTypeEnabledChanged: (NotificationType, Boolean) -> Unit,
    onNotificationTypeClicked: (NotificationType) -> Unit,
    onDeviceNotificationSettingsClicked: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            items.forEach { item ->
                NotificationTypeRow(
                    item = item,
                    onEnabledChanged = onNotificationTypeEnabledChanged,
                    onClick = onNotificationTypeClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(visible = !isAppNotificationsEnabled) {
                SystemNotificationsDisabledWarning(
                    onClick = onDeviceNotificationSettingsClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SystemNotificationsDisabledWarning(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_warning_filled_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        val textValue = stringResource(id = R.string.settings_notifs_app_notifications_disabled_warning)
        val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
        val highlightedTextStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary
        )
        val warningText = remember(textValue, highlightedTextStyle) {
            AnnotatedString.fromHtml(textValue).flatMapAnnotations { range ->
                if (range.item is LinkAnnotation) {
                    listOf(AnnotatedString.Range(highlightedTextStyle, range.start, range.end))
                } else {
                    listOf(range)
                }
            }
        }
        Text(
            text = warningText,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NotificationSettingsScreen(
    orderNotificationSoundStatus: NewOrderNotificationSoundStatus,
    onDeviceNotificationSettingsClicked: () -> Unit,
    onEnableChaChingSoundClicked: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            NotificationSettingsItem(
                title = stringResource(id = R.string.settings_notifs_device),
                subtitle = stringResource(id = R.string.settings_notifs_device_detail),
                onClick = onDeviceNotificationSettingsClicked,
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
private fun NotificationTypeRow(
    item: NotificationTypeItem,
    onEnabledChanged: (NotificationType, Boolean) -> Unit,
    onClick: (NotificationType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable { onClick(item.type) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(id = item.title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = item.subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        WCSwitch(
            checked = item.isEnabled,
            onCheckedChange = { onEnabledChanged(item.type, it) }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
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
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val NewOrderNotificationSoundStatus.requiresAttention: Boolean
    get() = this != NewOrderNotificationSoundStatus.DEFAULT

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SmarterNotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        SmarterNotificationSettingsScreen(
            items = listOf(
                NotificationTypeItem(
                    type = NotificationType.NEW_ORDERS,
                    title = R.string.settings_notifs_new_orders,
                    subtitle = R.string.settings_notifs_new_orders_subtitle,
                    isEnabled = true
                ),
                NotificationTypeItem(
                    type = NotificationType.NEW_REVIEWS,
                    title = R.string.settings_notifs_new_reviews,
                    subtitle = R.string.settings_notifs_new_reviews_subtitle,
                    isEnabled = true
                ),
                NotificationTypeItem(
                    type = NotificationType.STOCK,
                    title = R.string.settings_notifs_stock,
                    subtitle = R.string.settings_notifs_stock_subtitle,
                    isEnabled = true
                )
            ),
            isAppNotificationsEnabled = false,
            onNotificationTypeEnabledChanged = { _, _ -> },
            onNotificationTypeClicked = {},
            onDeviceNotificationSettingsClicked = {}
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        NotificationSettingsScreen(
            orderNotificationSoundStatus = NewOrderNotificationSoundStatus.DISABLED,
            onDeviceNotificationSettingsClicked = {},
            onEnableChaChingSoundClicked = {}
        )
    }
}
