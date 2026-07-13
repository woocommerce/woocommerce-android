package com.woocommerce.android.ui.dashboard.pushnotifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.dashboard.DashboardCardSurface
import com.woocommerce.android.ui.dashboard.DashboardOverflowMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.pushnotifications.WordPressWooBadge

@Composable
fun DashboardPushNotificationsCard(
    onHideClicked: () -> Unit,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onShown()
    }

    val menu = DashboardWidgetMenu(
        items = listOf(
            DashboardWidget.Type.PUSH_NOTIFICATIONS.defaultHideMenuEntry(onHideClicked)
        )
    )

    DashboardCardSurface(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = WooTheme.padding.padding5),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
        ) {
            WordPressWooBadge(
                iconSize = WooTheme.iconSize.size32,
                modifier = Modifier.padding(top = WooTheme.padding.padding5),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                Text(
                    text = stringResource(id = R.string.my_store_widget_push_notifications_title),
                    style = WooTheme.text.titleLarge.strong,
                    color = WooTheme.colors.surface.onDefault,
                )
                Text(
                    text = stringResource(id = R.string.my_store_widget_push_notifications_description),
                    style = WooTheme.text.bodyLarge.regular,
                    color = WooTheme.colors.surface.onDefault,
                    modifier = Modifier.padding(top = WooTheme.padding.padding2),
                )
            }
            DashboardOverflowMenu(
                items = menu.items,
                onSelected = { item -> item.action() },
                mapper = { it.title.getText() },
            )
        }
    }
}

@PreviewLightDark
@Composable
fun DashboardPushNotificationsCardPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardPushNotificationsCard(
            onHideClicked = {},
            onShown = {}
        )
    }
}
