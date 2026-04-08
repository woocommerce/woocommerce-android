package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize

@Composable
fun wooPosErrorAndEmptyStateButtonModifier(): Modifier {
    val configuration = LocalConfiguration.current
    val shortSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val isPhone = shortSide < 674
    return if (isPhone) {
        Modifier
            .fillMaxWidth()
            .height(80.dp.toAdaptiveComponentSize())
    } else {
        Modifier
            .fillMaxWidth(0.5f)
            .height(80.dp)
    }
}
