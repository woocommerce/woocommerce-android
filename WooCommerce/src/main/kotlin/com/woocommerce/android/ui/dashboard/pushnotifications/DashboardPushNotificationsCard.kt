package com.woocommerce.android.ui.dashboard.pushnotifications

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
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

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = colorResource(id = R.color.woo_gray_5),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WordPressWooBadge(
            iconSize = 32.dp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.my_store_widget_push_notifications_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(id = R.string.my_store_widget_push_notifications_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_50))
            )
        }
        WCOverflowMenu(
            items = menu.items,
            onSelected = { item -> item.action() },
            mapper = { it.title.getText() },
            tint = colorResource(id = R.color.color_on_surface_high)
        )
    }
}

@LightDarkThemePreviews
@Composable
fun DashboardPushNotificationsCardPreview() {
    WooThemeWithBackground {
        DashboardPushNotificationsCard(
            onHideClicked = {},
            onShown = {}
        )
    }
}
