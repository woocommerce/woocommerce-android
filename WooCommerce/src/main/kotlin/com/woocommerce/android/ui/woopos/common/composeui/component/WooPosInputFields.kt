package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.woocommerce.android.ui.compose.component.NullableCurrencyTextFieldValueMapper
import com.woocommerce.android.ui.payments.changeduecalculator.CurrencyVisualTransformation
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.WCSettingsModel
import java.math.BigDecimal
import kotlin.text.isEmpty

@Composable
fun WooPosMoneyInputField(
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    currencySymbol: String,
    currencyPosition: WCSettingsModel.CurrencyPosition,
    decimalSeparator: String,
    numberOfDecimals: Int,
    textStyle: WooPosTypography = WooPosTypography.BodyLarge,
    textColor: Color,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentAlignment: Alignment = Alignment.CenterStart,
) {
    val visualTransformation = remember {
        CurrencyVisualTransformation(
            currencySymbol = currencySymbol,
            currencyPosition = currencyPosition
        )
    }

    val visualTransformationWithoutCurrency = remember {
        CurrencyVisualTransformation(
            currencySymbol = "",
            currencyPosition = currencyPosition
        )
    }

    val valueMapper = NullableCurrencyTextFieldValueMapper.create(
        decimalSeparator = decimalSeparator,
        numberOfDecimals = numberOfDecimals
    )

    var currentValue by remember {
        mutableStateOf(value)
    }
    var textFieldValue by rememberSaveable(
        value != currentValue,
        stateSaver = TextFieldValue.Saver,
    ) {
        currentValue = value
        mutableStateOf(TextFieldValue(valueMapper.printValue(value)))
    }

    var labelWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier.background(WooPosTheme.colors.transparent),
        contentAlignment = contentAlignment,
    ) {
        val showLabel = textFieldValue.text.isEmpty()
        if (showLabel) {
            WooPosText(
                text = visualTransformation.filter(AnnotatedString("0.00")).text.toString(),
                style = textStyle,
                color = WooPosTheme.colors.onDisabledContainer,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    labelWidth = coordinates.size.width
                }
            )
        }

        val density = LocalDensity.current

        val textFieldModifier = if (showLabel) {
            Modifier.width(with(density) { labelWidth.toDp() + WooPosSpacing.XSmall.value })
        } else {
            Modifier.width(IntrinsicSize.Min)
        }

        val textFieldColor = if (showLabel) {
            textColor.copy(alpha = 0.2f)
        } else {
            textColor
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = onValueChange@{ updatedValue ->
                if (updatedValue.text == textFieldValue.text) {
                    textFieldValue = updatedValue
                    return@onValueChange
                }
                val transformedText = valueMapper.transformText(textFieldValue.text, updatedValue.text)
                runCatching { valueMapper.parseText(transformedText) }.onSuccess {
                    textFieldValue = TextFieldValue(
                        text = transformedText,
                        composition = updatedValue.composition,
                        selection = TextRange(
                            (updatedValue.selection.start + transformedText.length - updatedValue.text.length).coerceIn(
                                0,
                                transformedText.length
                            )
                        )
                    )

                    if (!valueMapper.equals(currentValue, it)) {
                        currentValue = it
                        onValueChange(it)
                    }
                }.onFailure {
                    WooLog.e(T.POS, "Failed to parse text: $transformedText", it)
                }
            },
            textStyle = textStyle.style.copy(color = textFieldColor),
            singleLine = true,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
            visualTransformation = VisualTransformation {
                if (it.text.isEmpty()) {
                    visualTransformationWithoutCurrency.filter(it)
                } else {
                    visualTransformation.filter(it)
                }
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            modifier = textFieldModifier,
        )
    }
}

@Composable
fun WooPosInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier,
    textStyle: WooPosTypography = WooPosTypography.BodyLarge,
    textColor: Color,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentAlignment: Alignment = Alignment.CenterStart,
) {
    var labelWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier.background(WooPosTheme.colors.transparent),
        contentAlignment = contentAlignment,
    ) {
        if (value.isEmpty()) {
            WooPosText(
                text = label,
                style = textStyle,
                color = WooPosTheme.colors.onDisabledContainer,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    labelWidth = coordinates.size.width
                }
            )
        }

        val density = LocalDensity.current

        // that's workaround to keep cursor to the left from the label
        val textFieldModifier = if (value.isEmpty()) {
            Modifier.width(with(density) { labelWidth.toDp() + WooPosSpacing.XSmall.value })
        } else {
            Modifier.width(IntrinsicSize.Min)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.style.copy(color = textColor),
            singleLine = true,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
            modifier = textFieldModifier,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosMoneyInputFieldPreview() {
    WooPosTheme {
        Column(modifier = Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosMoneyInputField(
                value = null,
                onValueChange = {},
                currencySymbol = "$",
                currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
                textColor = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

            WooPosMoneyInputField(
                value = BigDecimal.ZERO,
                onValueChange = {},
                currencySymbol = "$",
                currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
                textColor = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

            WooPosMoneyInputField(
                value = BigDecimal.TEN,
                onValueChange = {},
                currencySymbol = "$",
                currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
                textColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosInputFieldPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WooPosInputField(
                value = "longemail@gmail.com",
                onValueChange = {},
                contentAlignment = Alignment.Center,
                textColor = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

            WooPosInputField(
                value = "",
                onValueChange = {},
                label = "Label Label",
                textColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
