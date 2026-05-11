package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosMoneyInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import java.math.BigDecimal

@Composable
fun WooPosCustomAmountDialog(
    isVisible: Boolean,
    editing: WooPosCartItemViewState.CustomAmount?,
    onDismissRequest: () -> Unit,
    viewModel: WooPosCustomAmountDialogViewModel = hiltViewModel(),
) {
    LaunchedEffect(isVisible, editing?.itemNumber) {
        if (isVisible) {
            viewModel.initializeFor(editing)
        } else {
            viewModel.onDismissed()
        }
    }

    val state by viewModel.state.collectAsState()

    val dialogTitleRes = when (state.mode) {
        is WooPosCustomAmountDialogState.Mode.Edit -> R.string.woopos_custom_amount_dialog_title_edit
        WooPosCustomAmountDialogState.Mode.Add -> R.string.woopos_custom_amount_dialog_title_add
    }

    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(dialogTitleRes),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            WooPosText(
                text = stringResource(dialogTitleRes),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )

            AmountSection(
                state = state,
                onAmountChanged = viewModel::onAmountChanged,
            )

            NameSection(
                value = state.name,
                onNameChanged = viewModel::onNameChanged,
            )

            TaxesToggle(
                isTaxable = state.isTaxable,
                onToggled = viewModel::onTaxableToggled,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            DialogActions(
                state = state,
                onSubmit = {
                    viewModel.onSubmit()
                },
                onCancel = onDismissRequest,
            )
        }
    }
}

@Composable
private fun AmountSection(
    state: WooPosCustomAmountDialogState,
    onAmountChanged: (BigDecimal?) -> Unit,
) {
    Column {
        WooPosText(
            text = stringResource(R.string.woopos_custom_amount_dialog_amount_label),
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WooPosSpacing.Small.value),
            contentAlignment = Alignment.CenterStart,
        ) {
            WooPosMoneyInputField(
                value = state.amount,
                onValueChange = onAmountChanged,
                currencySymbol = state.currencySymbol,
                currencyPosition = state.currencyPosition,
                decimalSeparator = state.decimalSeparator,
                numberOfDecimals = state.numberOfDecimals,
                textStyle = WooPosTypography.Heading,
                textColor = MaterialTheme.colorScheme.onSurface,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                contentAlignment = Alignment.CenterStart,
            )
        }
    }
}

@Composable
private fun NameSection(
    value: String,
    onNameChanged: (String) -> Unit,
) {
    Column {
        WooPosText(
            text = stringResource(R.string.woopos_custom_amount_dialog_name_label),
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WooPosSpacing.Small.value),
            contentAlignment = Alignment.CenterStart,
        ) {
            WooPosInputField(
                value = value,
                onValueChange = onNameChanged,
                label = stringResource(R.string.woopos_custom_amount_dialog_name_placeholder),
                textColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TaxesToggle(
    isTaxable: Boolean,
    onToggled: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_custom_amount_dialog_charge_taxes),
            style = WooPosTypography.BodyMedium,
        )
        Switch(
            checked = isTaxable,
            onCheckedChange = onToggled,
        )
    }
}

@Composable
private fun DialogActions(
    state: WooPosCustomAmountDialogState,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        WooPosOutlinedButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.woopos_custom_amount_dialog_cancel),
            onClick = onCancel,
        )
        val submitText = when (state.mode) {
            is WooPosCustomAmountDialogState.Mode.Edit -> R.string.woopos_custom_amount_dialog_submit_edit
            WooPosCustomAmountDialogState.Mode.Add -> R.string.woopos_custom_amount_dialog_submit_add
        }
        WooPosButton(
            modifier = Modifier.weight(1f),
            text = stringResource(submitText),
            state = if (state.isSubmitEnabled) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
            onClick = onSubmit,
        )
    }
}
