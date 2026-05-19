package com.woocommerce.android.ui.prefs.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NotificationType
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NotificationTypeItem

@Composable
fun WooPushNotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    sharedViewModel: NotificationSettingsSharedViewModel
) {
    val notificationTypeItems = sharedViewModel.notificationTypeItems.observeAsState().value ?: return
    val isAppNotificationsEnabled = viewModel.isAppNotificationsEnabled.observeAsState().value ?: return
    val isNotificationSettingsLoading = sharedViewModel.isNotificationSettingsLoading.observeAsState().value ?: return
    val isNotificationTypeSelectionEnabled =
        sharedViewModel.isNotificationTypeSelectionEnabled.observeAsState().value
            ?.let { it && isAppNotificationsEnabled }
            ?: return

    WooPushNotificationSettingsScreen(
        items = notificationTypeItems,
        isAppNotificationsEnabled = isAppNotificationsEnabled,
        isNotificationSettingsLoading = isNotificationSettingsLoading,
        isNotificationTypeSelectionEnabled = isNotificationTypeSelectionEnabled,
        onNotificationTypeEnabledChanged = sharedViewModel::onNotificationTypeEnabledChanged,
        onNotificationTypeClicked = sharedViewModel::onNotificationTypeClicked,
        onDeviceNotificationSettingsClicked = viewModel::onDeviceNotificationSettingsClicked
    )
}

@Composable
private fun WooPushNotificationSettingsScreen(
    items: List<NotificationTypeItem>,
    isAppNotificationsEnabled: Boolean,
    isNotificationSettingsLoading: Boolean,
    isNotificationTypeSelectionEnabled: Boolean,
    onNotificationTypeEnabledChanged: (NotificationType, Boolean) -> Unit,
    onNotificationTypeClicked: (NotificationType) -> Unit,
    onDeviceNotificationSettingsClicked: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { paddingValues ->
        AnimatedVisibility(
            visible = isNotificationSettingsLoading,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            )
        }
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
                    onClick = { onNotificationTypeClicked(item.type) },
                    isNotificationTypeSelectionEnabled = isNotificationTypeSelectionEnabled,
                    isNotificationChannelEnabled = item.isNotificationChannelEnabled,
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
private fun NotificationTypeRow(
    item: NotificationTypeItem,
    onEnabledChanged: (NotificationType, Boolean) -> Unit,
    onClick: () -> Unit,
    isNotificationTypeSelectionEnabled: Boolean,
    isNotificationChannelEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val enabled = isNotificationTypeSelectionEnabled && isNotificationChannelEnabled
    val isChannelDisabledStateVisible = isNotificationTypeSelectionEnabled && !isNotificationChannelEnabled
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable(enabled = isNotificationTypeSelectionEnabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val contentAlpha = if (enabled) 1f else 0.38f
        val titleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(id = item.title),
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            if (!isChannelDisabledStateVisible) {
                Text(
                    text = stringResource(id = item.subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning_filled_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.settings_notifs_channel_disabled_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        WCSwitch(
            checked = item.isEnabled,
            onCheckedChange = { onEnabledChanged(item.type, it) },
            modifier = Modifier.clickable(enabled = isChannelDisabledStateVisible, onClick = onClick),
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24dp),
            contentDescription = null,
            tint = subtitleColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
@LightDarkThemePreviews
private fun WooPushNotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        WooPushNotificationSettingsScreen(
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
            isNotificationSettingsLoading = false,
            isNotificationTypeSelectionEnabled = true,
            onNotificationTypeEnabledChanged = { _, _ -> },
            onNotificationTypeClicked = {},
            onDeviceNotificationSettingsClicked = {}
        )
    }
}
