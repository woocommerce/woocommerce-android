package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.NullableCurrencyTextFieldValueMapper
import com.woocommerce.android.ui.payments.changeduecalculator.CurrencyVisualTransformation
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import java.math.BigDecimal

@Composable
fun WooPosCustomAmountDetailsScreen(
    state: WooPosCustomAmountFormState,
    onEditAmount: () -> Unit,
    onEditName: () -> Unit,
    onTaxableToggled: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosCustomAmountLayout(
        modifier = modifier,
        title = stringResource(R.string.woopos_custom_amount_details_title),
        buttonText = stringResource(
            when (state.mode) {
                is WooPosCustomAmountFormState.Mode.Edit -> R.string.woopos_custom_amount_dialog_submit_edit
                WooPosCustomAmountFormState.Mode.Add -> R.string.woopos_custom_amount_dialog_submit_add
            }
        ),
        onBackClicked = onBackClicked,
        onSaveClicked = onSubmit,
        buttonState = when {
            state.isSubmitting -> WooPosButtonState.LOADING
            state.isSubmitEnabled -> WooPosButtonState.ENABLED
            else -> WooPosButtonState.DISABLED
        },
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = WooPosSpacing.XLarge.value,
                    top = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.XLarge.value,
                ),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            EditableDetailCard(
                label = stringResource(R.string.woopos_custom_amount_dialog_amount_label),
                value = formattedAmount(state),
                enabled = !state.isSubmitting,
                onClick = onEditAmount,
            )
            EditableDetailCard(
                label = stringResource(R.string.woopos_custom_amount_name_optional),
                value = state.name.trim().ifBlank {
                    stringResource(R.string.woopos_custom_amount_dialog_name_placeholder)
                },
                isPlaceholder = state.name.isBlank(),
                enabled = !state.isSubmitting,
                onClick = onEditName,
            )
            DetailCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.isTaxable,
                            enabled = !state.isSubmitting,
                            role = Role.Switch,
                            onValueChange = onTaxableToggled,
                        )
                        .padding(WooPosSpacing.XLarge.value),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                ) {
                    DetailLabel(
                        text = stringResource(R.string.woopos_custom_amount_dialog_charge_taxes),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.isTaxable, enabled = !state.isSubmitting, onCheckedChange = null)
                }
            }
        }
    }
}

@Composable
private fun formattedAmount(state: WooPosCustomAmountFormState): String {
    val valueMapper = NullableCurrencyTextFieldValueMapper.create(
        decimalSeparator = state.decimalSeparator,
        numberOfDecimals = state.numberOfDecimals,
        keepTrailingZeros = true,
    )
    return CurrencyVisualTransformation(state.currencySymbol, state.currencyPosition)
        .filter(AnnotatedString(valueMapper.printValue(state.amount)))
        .text.text
}

@Composable
private fun EditableDetailCard(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isPlaceholder: Boolean = false,
) {
    val editLabel = stringResource(R.string.woopos_cart_custom_amount_edit_content_description, label)
    DetailCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = editLabel, onClick = onClick)
                .padding(WooPosSpacing.XLarge.value),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailLabel(text = label, modifier = Modifier.weight(1f))
            WooPosText(
                modifier = Modifier.weight(2f),
                text = value,
                style = WooPosTypography.BodyXLarge,
                fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isPlaceholder) WooPosTheme.colors.onDisabledContainer else Color.Unspecified,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WooPosCornerRadius.Large.value),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        content = content,
    )
}

@Composable
private fun DetailLabel(text: String, modifier: Modifier = Modifier) {
    WooPosText(
        modifier = modifier,
        text = text,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
}

@WooPosPreview
@Composable
fun WooPosCustomAmountDetailsScreenPreview() {
    WooPosTheme {
        WooPosCustomAmountDetailsScreen(
            state = WooPosCustomAmountFormState(amount = BigDecimal("40.00"), currencySymbol = "$"),
            onEditAmount = {},
            onEditName = {},
            onTaxableToggled = {},
            onSubmit = {},
            onBackClicked = {},
        )
    }
}
