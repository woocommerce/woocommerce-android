package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

/**
 * Renders the first character of [name] as an initials avatar on the surface container colour.
 * Used for custom amount rows — matches the iOS `CustomAmountAvatar`.
 */
@Composable
fun WooPosCustomAmountInitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    textStyle: WooPosTypography = WooPosTypography.Heading,
) {
    val initial = name.trim().firstOrNull()?.uppercase().orEmpty()
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        WooPosText(
            text = initial,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCustomAmountInitialsAvatarPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .size(WooPosComponentSize.Medium.value)
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value)),
        ) {
            WooPosCustomAmountInitialsAvatar(name = "Service fee")
        }
    }
}

@WooPosPreview
@Composable
fun WooPosCustomAmountInitialsAvatarBlankNamePreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .size(WooPosComponentSize.Medium.value)
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value)),
        ) {
            WooPosCustomAmountInitialsAvatar(name = "")
        }
    }
}
