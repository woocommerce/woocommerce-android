package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout

@Composable
fun wooPosErrorAndEmptyStateButtonModifier(): Modifier {
    val isPhone = isWooPosPhoneLayout()
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
