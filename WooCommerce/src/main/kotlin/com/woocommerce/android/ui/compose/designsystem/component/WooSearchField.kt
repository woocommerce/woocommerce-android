package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    onClearClick: (() -> Unit)? = null,
    clearContentDescription: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    if (onClearClick != null) {
        require(!clearContentDescription.isNullOrBlank()) {
            "WooSearchField clearContentDescription must not be blank when onClearClick is provided"
        }
    }

    val colors = WooTheme.colors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        textStyle = WooTheme.text.bodyMedium.regular,
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    style = WooTheme.text.bodyMedium.regular,
                )
            }
        },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_search_24dp),
                contentDescription = null,
            )
        },
        trailingIcon = if (onClearClick != null && value.isNotEmpty()) {
            {
                IconButton(
                    onClick = onClearClick,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                        contentDescription = clearContentDescription,
                        modifier = Modifier.size(SEARCH_ICON_SIZE),
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.surface.onDefault,
            unfocusedTextColor = colors.surface.onDefault,
            disabledTextColor = colors.surface.onLowest,
            focusedContainerColor = colors.surface.default,
            unfocusedContainerColor = colors.surface.default,
            disabledContainerColor = colors.surface.default,
            cursorColor = colors.primary,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
            disabledBorderColor = colors.outlineVariant,
            focusedLeadingIconColor = colors.surface.onVariant,
            unfocusedLeadingIconColor = colors.surface.onVariant,
            disabledLeadingIconColor = colors.surface.onLowest,
            focusedTrailingIconColor = colors.surface.onDefault,
            unfocusedTrailingIconColor = colors.surface.onDefault,
            disabledTrailingIconColor = colors.surface.onLowest,
            focusedPlaceholderColor = colors.surface.onVariant,
            unfocusedPlaceholderColor = colors.surface.onVariant,
            disabledPlaceholderColor = colors.surface.onLowest,
        ),
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooSearchFieldPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(modifier = Modifier.padding(WooTheme.padding.padding5)) {
                WooSearchField(
                    value = "Search products",
                    onValueChange = {},
                    onClearClick = {},
                    clearContentDescription = "Clear search",
                    modifier = Modifier.fillMaxWidth(),
                )
                WooSearchField(
                    value = "Disabled search",
                    onValueChange = {},
                    enabled = false,
                    onClearClick = {},
                    clearContentDescription = "Clear search",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val SEARCH_ICON_SIZE = 20.dp
