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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosMoneyInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
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

    when (currentWooPosBreakpoint()) {
        WooPosBreakpoint.Phone -> PhoneFullScreenLayout(
            isVisible = isVisible,
            state = state,
            dialogTitleRes = dialogTitleRes,
            onAmountChanged = viewModel::onAmountChanged,
            onNameChanged = viewModel::onNameChanged,
            onTaxableToggled = viewModel::onTaxableToggled,
            onSubmit = viewModel::onSubmit,
            onDismissRequest = onDismissRequest,
        )

        WooPosBreakpoint.SmallTablet,
        WooPosBreakpoint.Tablet -> TabletDialogLayout(
            isVisible = isVisible,
            state = state,
            dialogTitleRes = dialogTitleRes,
            onAmountChanged = viewModel::onAmountChanged,
            onNameChanged = viewModel::onNameChanged,
            onTaxableToggled = viewModel::onTaxableToggled,
            onSubmit = viewModel::onSubmit,
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun TabletDialogLayout(
    isVisible: Boolean,
    state: WooPosCustomAmountDialogState,
    dialogTitleRes: Int,
    onAmountChanged: (BigDecimal?) -> Unit,
    onNameChanged: (String) -> Unit,
    onTaxableToggled: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissRequest: () -> Unit,
) {
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

            AmountSection(state = state, onAmountChanged = onAmountChanged)
            NameSection(value = state.name, onNameChanged = onNameChanged)
            TaxesToggle(isTaxable = state.isTaxable, onToggled = onTaxableToggled)

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            DialogActions(
                state = state,
                breakpoint = WooPosBreakpoint.Tablet,
                onSubmit = onSubmit,
                onCancel = onDismissRequest,
            )
        }
    }
}

@Composable
private fun PhoneFullScreenLayout(
    isVisible: Boolean,
    state: WooPosCustomAmountDialogState,
    dialogTitleRes: Int,
    onAmountChanged: (BigDecimal?) -> Unit,
    onNameChanged: (String) -> Unit,
    onTaxableToggled: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!isVisible) return

    BackHandler(enabled = true) { onDismissRequest() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PhoneToolbar(
                titleRes = dialogTitleRes,
                onCloseClick = onDismissRequest,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = WooPosSpacing.Medium.value),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
            ) {
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                AmountSection(state = state, onAmountChanged = onAmountChanged)
                NameSection(value = state.name, onNameChanged = onNameChanged)
                TaxesToggle(isTaxable = state.isTaxable, onToggled = onTaxableToggled)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(WooPosSpacing.Medium.value),
            ) {
                DialogActions(
                    state = state,
                    breakpoint = WooPosBreakpoint.Phone,
                    onSubmit = onSubmit,
                    onCancel = onDismissRequest,
                )
            }
        }
    }
}

@Composable
private fun PhoneToolbar(
    titleRes: Int,
    onCloseClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(WooPosSpacing.Small.value),
    ) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.close),
                modifier = Modifier.size(WooPosIconSize.Large.value),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        WooPosText(
            text = stringResource(titleRes),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center),
        )
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
private fun DialogActions(
    state: WooPosCustomAmountDialogState,
    breakpoint: WooPosBreakpoint,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val submitText = when (state.mode) {
        is WooPosCustomAmountDialogState.Mode.Edit -> R.string.woopos_custom_amount_dialog_submit_edit
        WooPosCustomAmountDialogState.Mode.Add -> R.string.woopos_custom_amount_dialog_submit_add
    }
    val submitButtonState =
        if (state.isSubmitEnabled) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED

    when (breakpoint) {
        WooPosBreakpoint.Phone -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(submitText),
                state = submitButtonState,
                onClick = onSubmit,
            )
            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_custom_amount_dialog_cancel),
                onClick = onCancel,
            )
        }

        WooPosBreakpoint.SmallTablet,
        WooPosBreakpoint.Tablet -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            WooPosOutlinedButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.woopos_custom_amount_dialog_cancel),
                onClick = onCancel,
            )
            WooPosButton(
                modifier = Modifier.weight(1f),
                text = stringResource(submitText),
                state = submitButtonState,
                onClick = onSubmit,
            )
        }
    }
}
