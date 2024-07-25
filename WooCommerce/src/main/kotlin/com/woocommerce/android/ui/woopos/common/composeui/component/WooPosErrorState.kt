package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun WooPosErrorState(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Error,
    message: String,
    reason: String,
    primaryButton: Button? = null,
    secondaryButton: Button? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .padding(32.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = icon,
                contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
                tint = WooPosTheme.colors.error,
            )

            Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))

            Text(
                text = message,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.SemiBold
            )

            Divider(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier
                    .padding(vertical = 8.dp.toAdaptivePadding())
                    .widthIn(min = 150.dp.toAdaptivePadding())
            )

            Text(
                text = reason,
                style = MaterialTheme.typography.h5
            )

            Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))

            primaryButton?.let {
                WooPosButton(
                    text = it.text,
                    onClick = it.click,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp.toAdaptivePadding()))
            }

            secondaryButton?.let {
                WooPosButton(
                    text = it.text,
                    onClick = it.click,
                )
                Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
            }
        }
    }
}

data class Button(
    val text: String,
    val click: () -> Unit
)

@Composable
@WooPosPreview
fun WooPosErrorStatePreview() {
    WooPosTheme {
        WooPosErrorState(
            icon = Icons.Default.Error,
            message = stringResource(R.string.woopos_totals_main_error_label),
            reason = "Reason",
            primaryButton = Button(
                text = stringResource(R.string.retry),
                click = { }
            ),
            secondaryButton = Button(
                text = stringResource(R.string.cancel),
                click = { }
            )
        )
    }
}
