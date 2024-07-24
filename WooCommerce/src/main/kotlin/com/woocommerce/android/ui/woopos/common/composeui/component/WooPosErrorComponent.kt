package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosErrorComponent(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    message: String,
    reason: String,
    primaryButton: Button? = null,
    secondaryButton: Button? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically) // Align elements with spacing
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.size(56.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface
        )

        Divider(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = reason,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface
        )

        primaryButton?.let {
            Spacer(modifier = Modifier.height(32.dp))
            WooPosButton(
                text = it.text,
                onClick = it.click,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            )
        }

        secondaryButton?.let {
            Spacer(modifier = Modifier.height(16.dp))
            WooPosButton(
                text = it.text,
                onClick = it.click,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            )
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
    WooPosErrorComponent(
        icon = Icons.Default.Error,
        message = stringResource(R.string.woopos_totals_main_error_label),
        reason = "Reason",
        primaryButton = Button(
            text = stringResource(R.string.retry),
            click = { /* Handle click */ }
        ),
        secondaryButton = Button(
            text = stringResource(R.string.cancel),
            click = { /* Handle click */ }
        )
    )
}
