package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.Menu

@Composable
fun WooPosToolbarPopUpMenu(
    menuItems: List<Menu.MenuItem>,
    onClick: (Menu.MenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosCard(
        modifier = modifier.width(IntrinsicSize.Max),
        elevation = WooPosElevation.Medium,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            menuItems.forEach { menuItem ->
                WooPosToolbarPopUpMenuItem(menuItem, onClick)
            }
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
    }
}

@Composable
private fun WooPosToolbarPopUpMenuItem(
    menuItem: Menu.MenuItem,
    onClick: (Menu.MenuItem) -> Unit,
) {
    TextButton(onClick = { onClick(menuItem) }) {
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        Icon(
            imageVector = ImageVector.vectorResource(menuItem.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(WooPosIconSize.Small.value),
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        WooPosText(
            modifier = Modifier
                .padding(vertical = WooPosSpacing.Small.value)
                .weight(1f),
            text = stringResource(menuItem.title),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
    }
}
