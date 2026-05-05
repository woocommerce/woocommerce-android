package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
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
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.notifications.NewStockNotificationSettingsViewModel.StockNotificationType
import com.woocommerce.android.ui.prefs.notifications.NewStockNotificationSettingsViewModel.ViewState
import com.woocommerce.android.ui.prefs.notifications.compose.EnableNotificationsCard

@Composable
fun NewStockNotificationSettingsScreen(viewModel: NewStockNotificationSettingsViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        NewStockNotificationSettingsScreen(
            viewState = viewState,
            onStockNotificationEnabledChanged = viewModel::onStockNotificationEnabledChanged,
            onEditStoreSettingsClicked = viewModel::onEditStoreSettingsClicked
        )
    }
}

@Composable
private fun NewStockNotificationSettingsScreen(
    viewState: ViewState,
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
                title = stringResource(R.string.settings_notifs_stock_low_stock_title),
                isEnabled = viewState.lowStockNotificationsEnabled,
                onEnabledChanged = {
                    onStockNotificationEnabledChanged(StockNotificationType.LowStock, it)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                descriptionContent = {
                    Text(
                        text = stringResource(R.string.settings_notifs_stock_low_stock_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LowStockDetails(
                        defaultLowStockThreshold = viewState.defaultLowStockThreshold,
                        onEditStoreSettingsClicked = onEditStoreSettingsClicked
                    )
                }
            )
            EnableNotificationsCard(
                title = stringResource(R.string.settings_notifs_stock_out_of_stock_title),
                description = stringResource(R.string.settings_notifs_stock_out_of_stock_description),
                isEnabled = viewState.outOfStockNotificationsEnabled,
                onEnabledChanged = {
                    onStockNotificationEnabledChanged(StockNotificationType.OutOfStock, it)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            EnableNotificationsCard(
                title = stringResource(R.string.settings_notifs_stock_backorder_title),
                description = stringResource(R.string.settings_notifs_stock_backorder_description),
                isEnabled = viewState.backorderNotificationsEnabled,
                onEnabledChanged = {
                    onStockNotificationEnabledChanged(StockNotificationType.Backorder, it)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LowStockDetails(
    defaultLowStockThreshold: Int,
    onEditStoreSettingsClicked: () -> Unit
) {
    val text = clickableAnnotatedStringRes(
        stringResId = R.string.settings_notifs_stock_low_stock_threshold,
        onUrlClick = { onEditStoreSettingsClicked() },
        defaultLowStockThreshold
    )
    val linkAnnotation = text.getLinkAnnotations(start = 0, end = text.length).lastOrNull()?.item
    val openInNewIconId = "openInNewIcon"
    val textWithIcon = buildAnnotatedString {
        append(text)
        append(" ")
        if (linkAnnotation != null) {
            pushLink(linkAnnotation)
            appendInlineContent(openInNewIconId, "[Icon]")
            pop()
        } else {
            appendInlineContent(openInNewIconId, "[Icon]")
        }
    }
    val iconColor = MaterialTheme.colorScheme.primary
    val inlineContent = mapOf(
        openInNewIconId to InlineTextContent(
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

    Text(
        text = textWithIcon,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NewStockNotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        NewStockNotificationSettingsScreen(
            viewState = ViewState(),
            onStockNotificationEnabledChanged = { _, _ -> },
            onEditStoreSettingsClicked = {}
        )
    }
}
