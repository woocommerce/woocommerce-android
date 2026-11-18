package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint

@Composable
fun WooPosReceiptButtons(
    printReceiptButtonState: WooPosPrintReceiptButtonState,
    onEmailReceiptClicked: () -> Unit,
    onPrintReceiptClicked: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: WooPosTypography = WooPosTypography.BodyLarge,
) {
    if (printReceiptButtonState == WooPosPrintReceiptButtonState.Hidden) {
        EmailReceiptButton(textStyle, onEmailReceiptClicked, modifier)
        return
    }
    if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            EmailReceiptButton(textStyle, onEmailReceiptClicked, Modifier.fillMaxWidth())
            PrintReceiptButton(printReceiptButtonState, textStyle, onPrintReceiptClicked, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            EmailReceiptButton(textStyle, onEmailReceiptClicked, Modifier.weight(1f))
            PrintReceiptButton(printReceiptButtonState, textStyle, onPrintReceiptClicked, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmailReceiptButton(
    textStyle: WooPosTypography,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    WooPosOutlinedButton(
        modifier = modifier,
        text = stringResource(R.string.woopos_receipt_button),
        textStyle = textStyle,
        onClick = onClick,
    )
}

@Composable
private fun PrintReceiptButton(
    state: WooPosPrintReceiptButtonState,
    textStyle: WooPosTypography,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    WooPosOutlinedButton(
        modifier = modifier,
        text = stringResource(R.string.woopos_print_receipt_button),
        state = when (state) {
            WooPosPrintReceiptButtonState.Hidden,
            WooPosPrintReceiptButtonState.Idle -> WooPosButtonState.ENABLED

            WooPosPrintReceiptButtonState.Loading -> WooPosButtonState.LOADING
            WooPosPrintReceiptButtonState.Success -> WooPosButtonState.SUCCESS
        },
        textStyle = textStyle,
        onClick = onClick,
    )
}

@Composable
private fun ReceiptButtonsPreview(printReceiptButtonState: WooPosPrintReceiptButtonState) {
    WooPosTheme {
        WooPosReceiptButtons(
            printReceiptButtonState = printReceiptButtonState,
            onEmailReceiptClicked = {},
            onPrintReceiptClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Large.value),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosReceiptButtonsIdlePreview() {
    ReceiptButtonsPreview(WooPosPrintReceiptButtonState.Idle)
}

@WooPosPreview
@Composable
fun WooPosReceiptButtonsLoadingPreview() {
    ReceiptButtonsPreview(WooPosPrintReceiptButtonState.Loading)
}

@WooPosPreview
@Composable
fun WooPosReceiptButtonsSuccessPreview() {
    ReceiptButtonsPreview(WooPosPrintReceiptButtonState.Success)
}

@WooPosPreview
@Composable
fun WooPosReceiptButtonsHiddenPreview() {
    ReceiptButtonsPreview(WooPosPrintReceiptButtonState.Hidden)
}
