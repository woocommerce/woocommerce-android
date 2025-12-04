package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosIssueRefundDialog(
    isVisible: Boolean,
    orderId: Long,
    onDismissRequest: () -> Unit
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_orders_issue_refund_content_description
        ),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Large.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = stringResource(R.string.orderdetail_issue_refund_button),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(WooPosSpacing.Large.value))

            WooPosText(
                text = "Refund flow coming soon for order #$orderId",
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )

            Spacer(Modifier.height(WooPosSpacing.Large.value))
        }
    }
}

@WooPosPreview
@Composable
fun WooPosIssueRefundDialogPreview() {
    WooPosTheme {
        WooPosIssueRefundDialog(
            isVisible = true,
            orderId = 123L,
            onDismissRequest = {}
        )
    }
}
