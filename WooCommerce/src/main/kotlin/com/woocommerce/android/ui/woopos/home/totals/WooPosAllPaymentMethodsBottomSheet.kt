package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBottomSheet
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
internal fun WooPosAllPaymentMethodsBottomSheet(
    isVisible: Boolean,
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
) {
    WooPosBottomSheet(
        isVisible = isVisible,
        scrimContentDescription = stringResource(
            R.string.woopos_payment_method_picker_bottom_sheet_scrim_content_description
        ),
        onDismissRequest = onDismissRequest,
    ) {
        PaymentMethodsBottomSheetContent(
            methods = methods,
            onMethodClicked = onMethodClicked,
        )
    }
}

@Composable
private fun PaymentMethodsBottomSheetContent(
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WooPosSpacing.XLarge.value,
                end = WooPosSpacing.XLarge.value,
                top = WooPosSpacing.XLarge.value,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_payment_method_picker_bottom_sheet_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        PaymentMethodButtons(methods = methods, onMethodClicked = onMethodClicked)
    }
}

@Composable
private fun PaymentMethodButtons(
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        methods.forEach { method ->
            WooPosOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(method.testTag()),
                text = stringResource(method.labelRes()),
                onClick = { onMethodClicked(method) },
            )
        }
    }
}

@Composable
@WooPosPreview
fun PaymentMethodsBottomSheetReaderConnectedPreview() {
    WooPosTheme {
        WooPosAllPaymentMethodsBottomSheet(
            isVisible = true,
            methods = listOf(
                WooPosPaymentMethod.TAP_TO_PAY,
                WooPosPaymentMethod.SCAN_TO_PAY,
                WooPosPaymentMethod.MARK_ORDER_AS_PAID,
            ),
            onMethodClicked = {},
            onDismissRequest = {},
        )
    }
}

@Composable
@WooPosPreview
fun PaymentMethodsBottomSheetReaderDisconnectedPreview() {
    WooPosTheme {
        WooPosAllPaymentMethodsBottomSheet(
            isVisible = true,
            methods = listOf(
                WooPosPaymentMethod.CARD_READER,
                WooPosPaymentMethod.SCAN_TO_PAY,
                WooPosPaymentMethod.MARK_ORDER_AS_PAID,
            ),
            onMethodClicked = {},
            onDismissRequest = {},
        )
    }
}
