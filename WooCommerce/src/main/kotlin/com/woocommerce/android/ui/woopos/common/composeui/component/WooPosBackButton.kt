package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    contentDescription: String = stringResource(R.string.woopos_toolbar_icon_content_description),
    iconModifier: Modifier = Modifier.size(28.dp.toAdaptiveIconSize()),
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(WooPosIconSize.XLarge.value)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = iconModifier
        )
    }
}

@WooPosPreview
@Composable
fun WooPosBackButtonPreview() {
    WooPosTheme {
        WooPosBackButton {}
    }
}
