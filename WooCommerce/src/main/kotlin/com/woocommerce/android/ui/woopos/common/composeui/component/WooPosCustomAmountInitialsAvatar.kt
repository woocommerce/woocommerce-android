package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

/**
 * Renders the first character of [name] as an initials avatar on the primary container colour.
 * Used for custom amount rows in cart, order details, and refund flows — matches the iOS
 * `CustomAmountAvatar`.
 */
@Composable
fun WooPosCustomAmountInitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    textStyle: WooPosTypography = WooPosTypography.Heading,
) {
    val initial = name.trim().firstOrNull()?.uppercase().orEmpty()
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        WooPosText(
            text = initial,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
