package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
internal fun WooPosAllPaymentMethodsDialog(
    isVisible: Boolean,
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_payment_method_picker_dialog_title
        ),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_payment_method_picker_dialog_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
            methods.forEach { method ->
                WooPosOutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(method.testTag()),
                    text = stringResource(method.labelRes()),
                    onClick = { onMethodClicked(method) },
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            }
        }
    }
}
