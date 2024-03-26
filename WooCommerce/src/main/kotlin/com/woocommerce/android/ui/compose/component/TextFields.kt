package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection.Companion.ContentOrLtr
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

/**
 * An [OutlinedTextField] that displays an optional helper text below the field.
 */
@Composable
fun WCOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    helperText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
    placeholderText: String? = null
) {
    WCOutlinedTextFieldLayout(
        modifier = modifier,
        helperText = helperText,
        isError = isError
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(text = label)
            },
            enabled = enabled,
            readOnly = readOnly,
            modifier = textFieldModifier.fillMaxWidth(),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            colors = colors,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            placeholder = {
                placeholderText?.let {
                    Text(text = it)
                }
            }
        )
    }
}

/**
 * An [OutlinedTextField] that displays an optional helper text below the field.
 */
@Composable
fun WCOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
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
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
    placeholderText: String? = null
) {
    WCOutlinedTextFieldLayout(
        modifier = modifier,
        helperText = helperText,
        isError = isError
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(text = label)
            },
            enabled = enabled,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            colors = colors,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            interactionSource = interactionSource,
            placeholder = {
                placeholderText?.let {
                    Text(text = it)
                }
            }
        )
    }
}

@Composable
private fun WCOutlinedTextFieldLayout(
    helperText: String? = null,
    isError: Boolean = false,
    modifier: Modifier,
    textField: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        textField()
        if (!helperText.isNullOrEmpty()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.caption,
                color = if (!isError) {
                    colorResource(id = R.color.color_on_surface_medium)
                } else {
                    MaterialTheme.colors.error
                },
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Composable
fun WCSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    backgroundColor: Color = TextFieldDefaults
        .textFieldColors()
        .backgroundColor(enabled = true).value,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = colorResource(id = R.color.color_on_surface),
            textDirection = ContentOrLtr
        ),
        modifier = modifier
            .defaultMinSize(minHeight = dimensionResource(id = R.dimen.major_250))
            .background(
                backgroundColor,
                RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            ),
        singleLine = true,
        cursorBrush = SolidColor(colorResource(id = R.color.color_on_surface)),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colorResource(id = R.color.color_on_surface_medium)
                )

                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))

                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.subtitle1,
                            color = colorResource(id = R.color.color_on_surface_medium)
                        )
                    }

                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(id = R.string.clear),
                            tint = colorResource(id = R.color.color_on_surface_high)
                        )
                    }
                }
            }
        },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions
    )
}

@Composable
fun WCPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    helperText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isPasswordVisible: Boolean by remember { mutableStateOf(false) }

    WCOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        helperText = helperText,
        modifier = modifier,
        visualTransformation = if (!isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Password),
        keyboardActions = keyboardActions,
        trailingIcon = {
            val image = if (isPasswordVisible) {
                painterResource(id = org.wordpress.android.login.R.drawable.ic_password_visibility)
            } else {
                painterResource(id = org.wordpress.android.login.R.drawable.ic_password_visibility_off)
            }

            val description = if (isPasswordVisible) {
                stringResource(id = R.string.hide_password_content_description)
            } else {
                stringResource(id = R.string.show_password_content_description)
            }

            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(painter = image, description)
            }
        }
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WCOutlinedTextFieldPreview() {
    WooThemeWithBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WCOutlinedTextField(value = "", label = "Label", onValueChange = {})
            WCOutlinedTextField(value = "Value", label = "Label", onValueChange = {})
            WCOutlinedTextField(value = "Value", label = "Label", onValueChange = {}, enabled = false)
            WCOutlinedTextField(value = "Value", label = "Label", onValueChange = {}, helperText = "Helper Text")
            WCOutlinedTextField(
                value = "Value",
                label = "Label",
                onValueChange = {},
                helperText = "Helper Text",
                isError = true
            )
            WCSearchField(
                value = "test",
                onValueChange = {},
                hint = "Search",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
            )
        }
    }
}
