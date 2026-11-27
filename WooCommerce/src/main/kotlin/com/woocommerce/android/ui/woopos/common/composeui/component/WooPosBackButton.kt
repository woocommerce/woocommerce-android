package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize

@Composable
fun WooPosBackButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_back_24dp,
    contentDescription: String = stringResource(R.string.woopos_toolbar_icon_content_description),
    iconModifier: Modifier = Modifier.size(28.dp.toAdaptiveIconSize()),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = modifier.size(WooPosIconSize.XLarge.value)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = contentDescription,
            modifier = iconModifier
        )
    }
}

@Composable
fun WooPosCloseButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosBackButton(
        modifier = modifier,
        iconRes = R.drawable.ic_close_24dp,
        contentDescription = contentDescription,
        iconModifier = Modifier.size(32.dp.toAdaptiveIconSize()),
        onClick = onClick,
    )
}

@WooPosPreview
@Composable
fun WooPosBackButtonPreview() {
    WooPosTheme {
        WooPosBackButton {}
    }
}
