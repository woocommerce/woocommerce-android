package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.Menu
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole

@Composable
fun WooPosToolbarPopUpMenu(
    menuItems: List<Menu.MenuItem>,
    activeRole: WooPosLocalRole?,
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
            if (activeRole != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Small.value))
                WooPosToolbarPopUpMenuActiveRole(activeRole)
            }
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
    }
}

@Composable
private fun WooPosToolbarPopUpMenuActiveRole(role: WooPosLocalRole) {
    val title = stringResource(
        when (role) {
            WooPosLocalRole.ADMIN -> R.string.woopos_role_admin
            WooPosLocalRole.CASHIER -> R.string.woopos_role_cashier
        }
    )
    val description = stringResource(R.string.woopos_role_active, title)
    Row(
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = description }
            .padding(ButtonDefaults.TextButtonContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuIconAndLabel(R.drawable.ic_user_circle_24dp, title, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WooPosToolbarPopUpMenuItem(
    menuItem: Menu.MenuItem,
    onClick: (Menu.MenuItem) -> Unit,
) {
    TextButton(onClick = { onClick(menuItem) }, enabled = menuItem.enabled) {
        MenuIconAndLabel(
            icon = menuItem.icon,
            title = stringResource(menuItem.title),
            color = MaterialTheme.colorScheme.onSurface.let { if (menuItem.enabled) it else it.copy(alpha = 0.38f) },
            labelModifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MenuIconAndLabel(
    @DrawableRes icon: Int,
    title: String,
    color: Color,
    labelModifier: Modifier = Modifier,
) {
    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
    Icon(
        imageVector = ImageVector.vectorResource(icon),
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(WooPosIconSize.Small.value),
    )
    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
    WooPosText(
        modifier = labelModifier.padding(vertical = WooPosSpacing.Small.value),
        text = title,
        style = WooPosTypography.BodyMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
}
