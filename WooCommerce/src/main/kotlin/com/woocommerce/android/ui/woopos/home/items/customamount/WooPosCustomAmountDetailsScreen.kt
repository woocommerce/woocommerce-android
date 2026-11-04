package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.NullableCurrencyTextFieldValueMapper
import com.woocommerce.android.ui.payments.changeduecalculator.CurrencyVisualTransformation
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosFullScreenInputLayout
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import java.math.BigDecimal

@Composable
fun WooPosCustomAmountDetailsScreen(
    state: WooPosCustomAmountDialogState,
    onEditAmount: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTaxableToggled: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val submitText = when (state.mode) {
        WooPosCustomAmountDialogState.Mode.Add -> R.string.woopos_custom_amount_dialog_submit_add
        is WooPosCustomAmountDialogState.Mode.Edit -> R.string.woopos_custom_amount_dialog_submit_edit
    }
    WooPosFullScreenInputLayout(
        modifier = modifier.navigationBarsPadding(),
        titleText = stringResource(R.string.woopos_custom_amount_form_title),
        onBackClicked = onBackClicked,
        centerContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AmountRow(state = state, onEditAmount = onEditAmount)
                DetailsDivider()
                NameRow(value = state.name, onNameChanged = onNameChanged)
                DetailsDivider()
                WooPosCustomAmountTaxesToggle(isTaxable = state.isTaxable, onToggled = onTaxableToggled)
            }
        },
        buttonText = stringResource(submitText),
        buttonState = if (state.isSubmitEnabled) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
        onButtonClicked = onSubmit,
    )
}

@Composable
private fun AmountRow(state: WooPosCustomAmountDialogState, onEditAmount: () -> Unit) {
    val label = stringResource(R.string.woopos_custom_amount_dialog_amount_label)
    val editLabel = stringResource(R.string.woopos_cart_custom_amount_edit_content_description, label)
    val valueMapper = NullableCurrencyTextFieldValueMapper.create(
        decimalSeparator = state.decimalSeparator,
        numberOfDecimals = state.numberOfDecimals,
        keepTrailingZeros = true,
    )
    val formattedAmount = CurrencyVisualTransformation(state.currencySymbol, state.currencyPosition)
        .filter(AnnotatedString(valueMapper.printValue(state.amount)))
        .text.text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            .clickable(onClickLabel = editLabel, onClick = onEditAmount)
            .padding(WooPosSpacing.Medium.value),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailsLabel(text = label, modifier = Modifier.weight(1f))
        WooPosText(text = formattedAmount, style = WooPosTypography.BodyLarge)
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
            contentDescription = null,
            tint = WooPosTheme.colors.onSurfaceVariantHighest,
            modifier = Modifier.size(WooPosIconSize.Small.value),
        )
    }
}

@Composable
private fun NameRow(value: String, onNameChanged: (String) -> Unit) {
    val label = stringResource(R.string.woopos_custom_amount_dialog_name_label)
    val editLabel = stringResource(R.string.woopos_cart_custom_amount_edit_content_description, label)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            .clickable(onClickLabel = editLabel) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            .padding(WooPosSpacing.Medium.value),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailsLabel(text = label, modifier = Modifier.weight(1f))
        WooPosInputField(
            modifier = Modifier
                .weight(2f)
                .focusRequester(focusRequester),
            value = value,
            onValueChange = onNameChanged,
            label = stringResource(R.string.woopos_custom_amount_dialog_name_placeholder),
            textStyle = WooPosTypography.BodyLarge,
            textColor = MaterialTheme.colorScheme.onSurface,
            contentAlignment = Alignment.CenterEnd,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

@Composable
private fun DetailsLabel(text: String, modifier: Modifier = Modifier) {
    WooPosText(
        modifier = modifier,
        text = text,
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun DetailsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
        color = WooPosTheme.colors.outlineVariant,
    )
}

@WooPosPreview
@Composable
fun WooPosCustomAmountDetailsScreenPreview() {
    WooPosTheme {
        WooPosCustomAmountDetailsScreen(
            state = WooPosCustomAmountDialogState(amount = BigDecimal.TEN, currencySymbol = "$"),
            onEditAmount = {},
            onNameChanged = {},
            onTaxableToggled = {},
            onSubmit = {},
            onBackClicked = {},
        )
    }
}
