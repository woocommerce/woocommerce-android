package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing

internal val PIN_PHONE_COMPACT_BODY_HEIGHT = 380.dp

@Composable
internal fun WooPosPINPhoneLayout(
    header: @Composable () -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val verticalPadding = if (maxHeight < PIN_PHONE_COMPACT_BODY_HEIGHT) {
            WooPosSpacing.None.value
        } else {
            WooPosSpacing.Medium.value
        }
        val horizontalPadding = if (maxHeight < PIN_PHONE_COMPACT_BODY_HEIGHT) {
            WooPosSpacing.Small.value
        } else {
            WooPosSpacing.XLarge.value
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            header()
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}
