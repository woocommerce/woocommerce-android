package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosMoneyInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import java.math.BigDecimal

@Composable
fun WooPosCustomAmountFormScreen(
    editing: WooPosCartItemViewState.CustomAmount?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosCustomAmountDialogViewModel = hiltViewModel(),
) {
    LaunchedEffect(editing?.itemNumber) {
        viewModel.initializeFor(editing)
    }

    // Reset the VM init sentinel whenever the form leaves composition — covers back gestures, submit
    // success, and any external navigation. Without this, opening the form again for the same item
    // (or a fresh "add" after a cancel) would short-circuit `initializeFor` and reuse stale state.
    DisposableEffect(Unit) {
        onDispose { viewModel.onDismissed() }
    }

    BackHandler(enabled = true) { onBackClick() }

    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WooPosSpacing.Medium.value),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            AmountSection(state = state, onAmountChanged = viewModel::onAmountChanged)
            HorizontalDivider(color = WooPosTheme.colors.outlineVariant)
            TaxesToggle(isTaxable = state.isTaxable, onToggled = viewModel::onTaxableToggled)
            HorizontalDivider(color = WooPosTheme.colors.outlineVariant)
            NameSection(value = state.name, onNameChanged = viewModel::onNameChanged)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(WooPosSpacing.Medium.value),
        ) {
            FormSubmitButton(state = state, onSubmit = viewModel::onSubmit)
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
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isTaxable,
                role = Role.Switch,
                onValueChange = onToggled,
            )
            .padding(vertical = WooPosSpacing.Small.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_custom_amount_dialog_charge_taxes),
            style = WooPosTypography.BodyMedium,
        )
        Switch(
            checked = isTaxable,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.color_on_primary),
                checkedTrackColor = colorResource(R.color.color_primary),
                uncheckedThumbColor = colorResource(R.color.divider_color),
                uncheckedTrackColor = colorResource(R.color.color_surface_elevated),
                uncheckedBorderColor = colorResource(R.color.divider_color),
            ),
        )
    }
}

@Composable
private fun FormSubmitButton(
    state: WooPosCustomAmountDialogState,
    onSubmit: () -> Unit,
) {
    val submitText = when (state.mode) {
        is WooPosCustomAmountDialogState.Mode.Edit -> R.string.woopos_custom_amount_dialog_submit_edit
        WooPosCustomAmountDialogState.Mode.Add -> R.string.woopos_custom_amount_dialog_submit_add
    }
    val submitButtonState =
        if (state.isSubmitEnabled) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED
    WooPosButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(submitText),
        state = submitButtonState,
        onClick = onSubmit,
    )
}
