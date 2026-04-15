package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosErrorScreen(
    modifier: Modifier = Modifier,
    icon: ImageVector = WooPosIcons.ErrorX,
    message: String,
    reason: String,
    primaryButton: WooPosErrorScreenButtonState? = null,
    secondaryButton: WooPosErrorScreenButtonState? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            .padding(WooPosSpacing.XLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                modifier = Modifier.size(WooPosComponentSize.Small.value),
                imageVector = icon,
                contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
                tint = WooPosTheme.colors.unspecified,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

            WooPosText(
                text = message,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = reason,
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.XXLarge.value))
            primaryButton?.let {
                WooPosButton(
                    text = it.text,
                    onClick = it.click,
                    modifier = Modifier.wooPosFullScreenActionButton()
                )
            }
            secondaryButton?.let {
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosOutlinedButton(
                    text = it.text,
                    onClick = it.click,
                    modifier = Modifier.wooPosFullScreenActionButton()
                )
            }
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
    }
}

data class WooPosErrorScreenButtonState(
    val text: String,
    val click: () -> Unit
)

@Composable
@WooPosPreview
fun WooPosErrorStatePreview() {
    WooPosTheme {
        WooPosErrorScreen(
            message = stringResource(R.string.woopos_totals_main_error_label),
            reason = "Reason",
            primaryButton = WooPosErrorScreenButtonState(
                text = stringResource(R.string.retry),
                click = { }
            ),
            secondaryButton = WooPosErrorScreenButtonState(
                text = stringResource(R.string.cancel),
                click = { }
            )
        )
    }
}

@Composable
@WooPosPreview
fun WooPosErrorStateSingleButtonPreview() {
    WooPosTheme {
        WooPosErrorScreen(
            message = "Very long title Very long title Very long title Very long title Very long title ",
            reason = "Very loooong reason Very loooong reason Very loooong reason Very loooong reason ",
            primaryButton = WooPosErrorScreenButtonState(
                text = stringResource(R.string.retry),
                click = { }
            ),
        )
    }
}
