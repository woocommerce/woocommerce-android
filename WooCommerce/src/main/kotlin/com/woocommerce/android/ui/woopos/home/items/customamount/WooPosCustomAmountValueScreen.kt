package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosMoneyInputField
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import java.math.BigDecimal

@Composable
fun WooPosCustomAmountValueScreen(
    state: WooPosCustomAmountDialogState,
    onAmountChanged: (BigDecimal?) -> Unit,
    onSaveClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveAmount = {
        // The view model ignores an invalid amount; keep the keyboard open so the merchant can fix it.
        if (state.isAmountValid) keyboardController?.hide()
        onSaveClicked()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    WooPosCustomAmountLayout(
        modifier = modifier,
        title = stringResource(R.string.woopos_custom_amount_dialog_amount_label),
        onBackClicked = onBackClicked,
        content = { contentModifier ->
            Box(
                modifier = contentModifier.verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                WooPosMoneyInputField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = state.amount,
                    onValueChange = onAmountChanged,
                    currencySymbol = state.currencySymbol,
                    currencyPosition = state.currencyPosition,
                    decimalSeparator = state.decimalSeparator,
                    numberOfDecimals = state.numberOfDecimals,
                    textStyle = WooPosTypography.Heading,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { saveAmount() }),
                    preselectText = true,
                )
            }
        },
        buttonState = if (state.isAmountValid) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
        onSaveClicked = saveAmount,
    )
}

@WooPosPreview
@Composable
fun WooPosCustomAmountValueScreenPreview() {
    WooPosTheme {
        WooPosCustomAmountValueScreen(
            state = WooPosCustomAmountDialogState(amount = BigDecimal("10.00"), currencySymbol = "$"),
            onAmountChanged = {},
            onSaveClicked = {},
            onBackClicked = {},
        )
    }
}
