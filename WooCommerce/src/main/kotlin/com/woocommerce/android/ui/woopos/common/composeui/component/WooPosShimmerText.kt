package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosShimmerText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    cornerRadius: WooPosCornerRadius? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val measuredText = textMeasurer.measure(
        text = text,
        style = style
    )

    val width = with(density) { measuredText.size.width.toDp() }
    val height = with(density) { measuredText.size.height.toDp() }

    val calculatedCornerRadius = cornerRadius ?: height.toCornerRadius()

    WooPosShimmerBox(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(calculatedCornerRadius.value))
    )
}

private fun Dp.toCornerRadius(): WooPosCornerRadius = when {
    this <= 20.dp -> WooPosCornerRadius.XSmall
    this <= 36.dp -> WooPosCornerRadius.Small
    this <= 54.dp -> WooPosCornerRadius.Medium
    this <= 80.dp -> WooPosCornerRadius.Large
    else -> WooPosCornerRadius.XLarge
}

@WooPosPreview
@Preview
@Composable
private fun WooPosShimmerTextPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            WooPosShimmerText(
                text = "Loading heading text...",
                style = WooPosTypography.Heading.style,
                modifier = Modifier.padding(bottom = WooPosSpacing.Small.value)
            )

            WooPosShimmerText(
                text = "Loading body large text",
                style = WooPosTypography.BodyLarge.style,
                modifier = Modifier.padding(bottom = WooPosSpacing.Small.value)
            )

            WooPosShimmerText(
                text = "Loading body medium text",
                style = WooPosTypography.BodyMedium.style,
                modifier = Modifier.padding(bottom = WooPosSpacing.Small.value)
            )

            WooPosShimmerText(
                text = "Loading caption",
                style = WooPosTypography.Caption.style,
                modifier = Modifier.padding(bottom = WooPosSpacing.Small.value)
            )

            WooPosShimmerText(
                text = "Custom corner radius",
                style = WooPosTypography.BodyLarge.style,
                cornerRadius = WooPosCornerRadius.None,
                modifier = Modifier.padding(bottom = WooPosSpacing.Small.value)
            )
        }
    }
}
