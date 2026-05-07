package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.notifications.NewStockNotificationSettingsViewModel.StockNotificationType
import com.woocommerce.android.ui.prefs.notifications.NewStockNotificationSettingsViewModel.ViewState
import com.woocommerce.android.ui.prefs.notifications.compose.EnableNotificationsCard

@Composable
fun NewStockNotificationSettingsScreen(viewModel: NewStockNotificationSettingsViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        NewStockNotificationSettingsScreen(
            viewState = viewState,
            onNotificationsEnabledChanged = viewModel::onNotificationsEnabledChanged,
            onStockNotificationEnabledChanged = viewModel::onStockNotificationEnabledChanged,
            onEditStoreSettingsClicked = viewModel::onEditStoreSettingsClicked
        )
    }
}

@Composable
private fun NewStockNotificationSettingsScreen(
    viewState: ViewState,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onStockNotificationEnabledChanged: (StockNotificationType, Boolean) -> Unit,
    onEditStoreSettingsClicked: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            EnableNotificationsCard(
                title = stringResource(R.string.settings_notifs_stock_enable_title),
                description = stringResource(R.string.settings_notifs_stock_enable_description),
                isEnabled = viewState.notificationsEnabled,
                onEnabledChanged = onNotificationsEnabledChanged
            )
            StockNotificationOption(
                title = stringResource(R.string.settings_notifs_stock_low_stock_title),
                description = stringResource(R.string.settings_notifs_stock_low_stock_description),
                checked = viewState.lowStockNotificationsEnabled,
                enabled = viewState.notificationsEnabled,
                onCheckedChange = {
                    onStockNotificationEnabledChanged(StockNotificationType.LowStock, it)
                }
            ) {
                LowStockDetails(
                    defaultLowStockThreshold = viewState.defaultLowStockThreshold,
                    enabled = viewState.notificationsEnabled,
                    onEditStoreSettingsClicked = onEditStoreSettingsClicked
                )
            }
            HorizontalDivider()
            StockNotificationOption(
                title = stringResource(R.string.settings_notifs_stock_out_of_stock_title),
                description = stringResource(R.string.settings_notifs_stock_out_of_stock_description),
                checked = viewState.outOfStockNotificationsEnabled,
                enabled = viewState.notificationsEnabled,
                onCheckedChange = {
                    onStockNotificationEnabledChanged(StockNotificationType.OutOfStock, it)
                }
            )
            HorizontalDivider()
            StockNotificationOption(
                title = stringResource(R.string.settings_notifs_stock_backorder_title),
                description = stringResource(R.string.settings_notifs_stock_backorder_description),
                checked = viewState.backorderNotificationsEnabled,
                enabled = viewState.notificationsEnabled,
                onCheckedChange = {
                    onStockNotificationEnabledChanged(StockNotificationType.Backorder, it)
                }
            )
        }
    }
}

@Composable
private fun StockNotificationOption(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val titleColor = MaterialTheme.colorScheme.onSurface.let {
            if (enabled) it else it.copy(alpha = 0.38f)
        }
        val descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant.let {
            if (enabled) it else it.copy(alpha = 0.38f)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = descriptionColor,
                modifier = Modifier.padding(top = 4.dp)
            )
            content()
        }
        WCSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun LowStockDetails(
    defaultLowStockThreshold: Int?,
    enabled: Boolean,
    onEditStoreSettingsClicked: () -> Unit
) {
    val thresholdPlaceholderId = "thresholdPlaceholder"
    val thresholdText = defaultLowStockThreshold ?: LOW_STOCK_THRESHOLD_PLACEHOLDER
    val text = clickableAnnotatedStringRes(
        stringResId = R.string.settings_notifs_stock_low_stock_threshold,
        onUrlClick = {
            if (enabled) {
                onEditStoreSettingsClicked()
            }
        },
        thresholdText
    )
    val openInNewIconId = "openInNewIcon"
    val textWithIcon = remember(text) {
        val linkAnnotation = text.getLinkAnnotations(start = 0, end = text.length).lastOrNull()?.item
        val thresholdPlaceholderStart = text.text.indexOf(LOW_STOCK_THRESHOLD_PLACEHOLDER)
        val thresholdPlaceholderEnd = thresholdPlaceholderStart + LOW_STOCK_THRESHOLD_PLACEHOLDER.length
        buildAnnotatedString {
            if (thresholdPlaceholderStart >= 0) {
                append(text.subSequence(0, thresholdPlaceholderStart))
                appendInlineContent(thresholdPlaceholderId, "[Threshold]")
                append(text.subSequence(thresholdPlaceholderEnd, text.length))
            } else {
                append(text)
            }
            append(" ")
            if (linkAnnotation != null) {
                pushLink(linkAnnotation)
                appendInlineContent(openInNewIconId, "[Icon]")
                pop()
            } else {
                appendInlineContent(openInNewIconId, "[Icon]")
            }
        }
    }
    val iconColor = MaterialTheme.colorScheme.primary
    val inlineContent = buildMap {
        if (defaultLowStockThreshold == null) {
            put(
                thresholdPlaceholderId,
                InlineTextContent(
                    Placeholder(
                        width = 8.sp,
                        height = 12.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 1.5.dp
                    )
                }
            )
        }
        put(
            openInNewIconId,
            InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = null,
                    tint = iconColor
                )
            }
        )
    }

    Text(
        text = textWithIcon,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .padding(top = 8.dp)
            .alpha(if (enabled) 1f else 0.38f)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NewStockNotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        NewStockNotificationSettingsScreen(
            viewState = ViewState(),
            onNotificationsEnabledChanged = {},
            onStockNotificationEnabledChanged = { _, _ -> },
            onEditStoreSettingsClicked = {}
        )
    }
}

private const val LOW_STOCK_THRESHOLD_PLACEHOLDER = "{lowStockThreshold}"
