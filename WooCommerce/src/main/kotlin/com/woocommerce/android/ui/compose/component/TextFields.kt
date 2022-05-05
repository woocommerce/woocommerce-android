package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
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
    helperText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    Column(modifier = modifier) {
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
        )
        if (!helperText.isNullOrEmpty()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.caption,
                color = if (!isError) colorResource(id = R.color.color_on_surface_medium)
                else MaterialTheme.colors.error,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WCOutlinedTextFieldPreview() {
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
        }
    }
}
