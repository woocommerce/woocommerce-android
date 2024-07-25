package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        Spacer(modifier = Modifier.height(8.dp.toAdaptivePadding()))

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

data class Button(
    val text: String,
    val click: () -> Unit
)

@WooPosPreview
@Composable
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
