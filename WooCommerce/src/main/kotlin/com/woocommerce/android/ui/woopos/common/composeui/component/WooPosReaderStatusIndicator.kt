package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosReaderIndicatorDot(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
fun WooPosReaderStatusText(
    title: String,
    modifier: Modifier = Modifier,
) {
    WooPosText(
        modifier = modifier.padding(horizontal = WooPosSpacing.Small.value),
        text = title,
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
