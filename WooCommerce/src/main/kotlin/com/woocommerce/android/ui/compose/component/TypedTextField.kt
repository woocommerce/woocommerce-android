package com.woocommerce.android.ui.compose.component

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A generic [OutlinedTextField] that accepts a typed value of type [T], and have a listener that emits values of the
 * same type.
 *
 * @param valueMapper the [TextFieldValueMapper] to use with this field.
 */
@Composable
fun <T> WCOutlinedTypedTextField(
    value: T,
    onValueChange: (T) -> Unit,
    label: String,
    valueMapper: TextFieldValueMapper<T>,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
    placeholderText: String? = null
) {
    var currentValue by remember {
        mutableStateOf(value)
    }
    // Monitor the passed value using the remember's key
    var textFieldValue by remember(value != currentValue) {
        currentValue = value
        mutableStateOf(TextFieldValue(valueMapper.printValue(value)))
    }

    WCOutlinedTextField(
        value = textFieldValue,
        onValueChange = onValueChange@{ updatedValue ->
            if (updatedValue.text == textFieldValue.text) {
                textFieldValue = updatedValue
                return@onValueChange
            }
            val transformedText = valueMapper.transformText(textFieldValue.text, updatedValue.text)
            runCatching { valueMapper.parseText(transformedText) }
                .onSuccess {
                    textFieldValue = TextFieldValue(
                        text = transformedText,
                        composition = updatedValue.composition,
                        // Update selection to preserve cursor position after text transformations
                        selection = TextRange(
                            (updatedValue.selection.start + transformedText.length - updatedValue.text.length)
                                .coerceIn(0, transformedText.length)
                        )
                    )
                    if (!valueMapper.equals(currentValue, it)) {
                        currentValue = it
                        onValueChange(it)
                    }
                }
        },
        label = label,
        modifier = modifier,
        helperText = helperText,
        enabled = enabled,
        readOnly = readOnly,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        interactionSource = interactionSource,
        colors = colors,
        placeholderText = placeholderText
    )
}

/**
 * Handles the mapping between the values of type [T] and their text representation, when used in a
 * typed [WCOutlinedTypedTextField].
 */
interface TextFieldValueMapper<T> {
    /**
     * Parse the passed [text] into a value of type [T].
     * When this throws an [Exception], the last text edit will be reverted.
     */
    @Throws(Exception::class)
    fun parseText(text: String): T

    /**
     * Returns a String representation of the instance [value], as it should be printed to the text field.
     */
    fun printValue(value: T): String

    /**
     * Handles any text transformations before parsing the text.
     * This can be useful for cases when we want to filter the allowed characters, or for advanced text manipulations
     * (such as: disallowing empty values, advanced decimal formatting...)
     */
    fun transformText(oldText: String, newText: String): String = newText

    /**
     * Checks whether the old value that the text field had [oldValue] equals the [newValue]
     * The Text field won't emit changes until the values are different
     */
    fun equals(oldValue: T, newValue: T): Boolean {
        return if (oldValue is Comparable<*> && newValue != null) {
            @Suppress("UNCHECKED_CAST")
            (oldValue as Comparable<Any>).compareTo(newValue) == 0
        } else {
            oldValue == newValue
        }
    }
}

class BigDecimalTextFieldValueMapper private constructor(
    private val supportsNegativeValue: Boolean = true
) : TextFieldValueMapper<BigDecimal> {
    override fun parseText(text: String): BigDecimal = text.toBigDecimal()
    override fun printValue(value: BigDecimal): String = value.toPlainString()
    override fun transformText(oldText: String, newText: String): String {
        val clearedText = if (!supportsNegativeValue) newText.filter { it != '-' } else newText
        return when {
            clearedText.isEmpty() || clearedText == "-" -> "0"
            clearedText.matches("^-?0+\\d".toRegex()) ->
                // Delete any leading 0s, since this field can't be cleared
                clearedText.replace("^(-?)0+".toRegex(), "$1")
            clearedText.toBigDecimalOrNull() != null -> clearedText
            else -> oldText
        }
    }

    companion object {
        @Composable
        fun create(supportsNegativeValue: Boolean) = remember(supportsNegativeValue) {
            BigDecimalTextFieldValueMapper(supportsNegativeValue)
        }
    }
}

class NullableBigDecimalTextFieldValueMapper private constructor(
    private val supportsNegativeValue: Boolean = true
) : TextFieldValueMapper<BigDecimal?> {
    override fun parseText(text: String): BigDecimal? = text.toBigDecimalOrNull()
    override fun printValue(value: BigDecimal?): String = value?.toPlainString().orEmpty()
    override fun transformText(oldText: String, newText: String): String {
        val clearedText = if (!supportsNegativeValue) newText.filter { it != '-' } else newText
        return when {
            clearedText.isEmpty() -> ""
            clearedText == "0" || clearedText == "-" || clearedText == "." -> clearedText
            clearedText.toBigDecimalOrNull() != null -> clearedText
            else -> oldText
        }
    }

    companion object {
        @Composable
        fun create(supportsNegativeValue: Boolean) = remember(supportsNegativeValue) {
            NullableBigDecimalTextFieldValueMapper(supportsNegativeValue)
        }
    }
}

class NullableCurrencyTextFieldValueMapper @VisibleForTesting constructor(
    private val decimalSeparator: String,
    private val numberOfDecimals: Int
) : TextFieldValueMapper<BigDecimal?> {
    private val acceptedChars = "0123456789.$decimalSeparator"

    override fun parseText(text: String): BigDecimal? =
        text.replace(decimalSeparator, ".").toBigDecimalOrNull()

    override fun printValue(value: BigDecimal?): String =
        value?.setScale(numberOfDecimals, RoundingMode.HALF_UP)
            ?.stripTrailingZeros()
            ?.toPlainString()
            ?.replace(".", decimalSeparator)
            .orEmpty()

    override fun transformText(oldText: String, newText: String): String {
        val clearedText = newText.filter { it in acceptedChars }
        return when {
            clearedText.isEmpty() || clearedText == decimalSeparator || clearedText == "." -> clearedText
            clearedText.hasAllowedNumberOfDecimals() && clearedText.replace(decimalSeparator, ".")
                .toBigDecimalOrNull() != null -> clearedText

            else -> oldText
        }
    }

    private fun String.hasAllowedNumberOfDecimals() = substringAfter(decimalSeparator, "").length <= numberOfDecimals

    companion object {
        @Composable
        fun create(decimalSeparator: String, numberOfDecimals: Int) = remember(decimalSeparator, numberOfDecimals) {
            NullableCurrencyTextFieldValueMapper(decimalSeparator, numberOfDecimals)
        }
    }
}

class NullableIntTextFieldValueMapper(
    private val supportsNegativeValue: Boolean = true
) : TextFieldValueMapper<Int?> {
    override fun parseText(text: String): Int? = text.toIntOrNull()
    override fun printValue(value: Int?): String = value?.toString().orEmpty()
    override fun transformText(oldText: String, newText: String): String {
        val clearedText = if (!supportsNegativeValue) newText.filter { it != '-' } else newText
        return when {
            clearedText.isEmpty() -> ""
            clearedText == "-" -> clearedText
            clearedText.matches("^-?0+\\d".toRegex()) ->
                // Delete any leading 0s, since this field can't be cleared
                clearedText.replace("^(-?)0+".toRegex(), "$1")
            clearedText.toIntOrNull() != null -> clearedText
            else -> oldText
        }
    }
}

@Preview
@Composable
private fun PreviewTypedTextFields() {
    WooThemeWithBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var signedDecimal by remember {
                mutableStateOf(BigDecimal.ZERO)
            }

            WCOutlinedTypedTextField(
                value = signedDecimal,
                onValueChange = { signedDecimal = it },
                label = "Signed BigDecimal",
                valueMapper = BigDecimalTextFieldValueMapper.create(supportsNegativeValue = true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            var nonSignedDecimal by remember {
                mutableStateOf(BigDecimal.ZERO)
            }
            WCOutlinedTypedTextField(
                value = nonSignedDecimal,
                onValueChange = { nonSignedDecimal = it },
                label = "Non-signed BigDecimal",
                valueMapper = BigDecimalTextFieldValueMapper.create(supportsNegativeValue = false),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            var optionalDecimal by remember {
                mutableStateOf<BigDecimal?>(null)
            }
            WCOutlinedTypedTextField(
                value = optionalDecimal,
                onValueChange = { optionalDecimal = it },
                label = "Optional BigDecimal",
                valueMapper = NullableBigDecimalTextFieldValueMapper.create(supportsNegativeValue = true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
