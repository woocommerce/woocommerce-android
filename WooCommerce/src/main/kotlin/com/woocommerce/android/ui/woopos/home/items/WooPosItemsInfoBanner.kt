package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosItemsInfoBanner(
    visible: Boolean,
    title: String,
    message: String,
    dismissContentDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
            scaleIn(animationSpec = tween(durationMillis = 180))
    ) {
        WooPosCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.Medium.value),
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
            elevation = WooPosElevation.Medium,
            shadowType = ShadowType.Soft,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_woo_pos_info_banner),
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null,
                    modifier = Modifier
                        .size(WooPosIconSize.XLarge.value)
                        .align(Alignment.CenterVertically)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    WooPosText(
                        text = title,
                        style = WooPosTypography.BodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    WooPosText(
                        text = message,
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.Top)
                        .offset(y = (-12).dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                        contentDescription = dismissContentDescription,
                        modifier = Modifier.size(WooPosIconSize.Medium.value),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosItemsInfoBannerPreview() {
    WooPosTheme {
        WooPosItemsInfoBanner(
            visible = true,
            title = "Heads up",
            message = "This is an informational banner shown at the top of the items list.",
            dismissContentDescription = "Dismiss",
            onDismiss = {}
        )
    }
}
