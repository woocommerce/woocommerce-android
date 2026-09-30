package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.MagnifyingGlass
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark

@Composable
@Suppress("CyclomaticComplexMethod")
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
    focusRequester: FocusRequester? = null,
    trailingActionText: String? = null,
    onTrailingActionClick: (() -> Unit)? = null,
    trailingActionEnabled: Boolean = enabled,
) {
    if (onClearClick != null) {
        assert(!clearContentDescription.isNullOrBlank()) {
            "WooSearchField clearContentDescription must not be blank when onClearClick is provided"
        }
    }
    if (trailingActionText != null || onTrailingActionClick != null) {
        require(!trailingActionText.isNullOrBlank()) {
            "WooSearchField trailingActionText must not be blank when onTrailingActionClick is provided"
        }
        require(onTrailingActionClick != null) {
            "WooSearchField onTrailingActionClick must be provided when trailingActionText is provided"
        }
    }

    val colors = WooTheme.colors
    val style = wooSearchFieldStyle(enabled = enabled, colors = colors)
    val showClearButton = onClearClick != null && value.isNotEmpty()

    Row(
        modifier = modifier
            .height(SEARCH_SHELL_HEIGHT)
            .background(style.shellColor)
            .padding(horizontal = WooTheme.padding.padding7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(SEARCH_ACTION_TOUCH_TARGET_SIZE)
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .height(SEARCH_FIELD_HEIGHT)
                    .fillMaxWidth()
                    .background(
                        color = style.fieldContainerColor,
                        shape = RoundedCornerShape(WooTheme.radius.large),
                    )
                    .padding(horizontal = WooTheme.padding.padding5),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = WooIcons.Regular.MagnifyingGlass,
                    contentDescription = null,
                    modifier = Modifier.size(WooTheme.iconSize.size18),
                    tint = style.iconColor,
                )

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .padding(start = WooTheme.spacing.space4)
                        .then(
                            if (focusRequester != null) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .weight(1f),
                    enabled = enabled,
                    singleLine = true,
                    textStyle = WooTheme.text.bodyLarge.regular.copy(color = style.textColor),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    color = style.placeholderColor,
                                    style = WooTheme.text.bodyLarge.regular,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                if (showClearButton) {
                    Spacer(modifier = Modifier.width(SEARCH_CLEAR_RESERVED_WIDTH))
                }
            }

            if (showClearButton) {
                WooSearchClearButton(
                    onClick = onClearClick,
                    enabled = enabled,
                    contentDescription = clearContentDescription.orEmpty(),
                    color = style.clearIconColor,
                    iconColor = style.fieldContainerColor,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        if (trailingActionText != null && onTrailingActionClick != null) {
            WooSearchTrailingAction(
                text = trailingActionText,
                onClick = onTrailingActionClick,
                enabled = enabled && trailingActionEnabled,
                modifier = Modifier.padding(start = WooTheme.spacing.space7),
            )
        }
    }
}

internal data class WooSearchFieldStyle(
    val shellColor: Color,
    val fieldContainerColor: Color,
    val textColor: Color,
    val iconColor: Color,
    val placeholderColor: Color,
    val clearIconColor: Color,
)

internal fun wooSearchFieldStyle(enabled: Boolean, colors: WooColors): WooSearchFieldStyle {
    val contentColor = if (enabled) colors.surface.onDefault else colors.surface.onVariantLowest
    return WooSearchFieldStyle(
        shellColor = colors.surface.bright,
        fieldContainerColor = colors.surface.surfaceDim,
        textColor = contentColor,
        iconColor = contentColor,
        placeholderColor = colors.stateLayers.onSurface.opacity24,
        clearIconColor = contentColor,
    )
}

@Composable
private fun WooSearchClearButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .size(SEARCH_ACTION_TOUCH_TARGET_SIZE)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(WooTheme.iconSize.size16),
            shape = CircleShape,
            color = color,
        ) {
            Icon(
                imageVector = WooIcons.Regular.Xmark,
                contentDescription = null,
                modifier = Modifier
                    .padding(SEARCH_CLEAR_ICON_PADDING)
                    .size(SEARCH_CLEAR_GLYPH_SIZE),
                tint = iconColor,
            )
        }
    }
}

@Composable
private fun WooSearchTrailingAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.widthIn(max = SEARCH_TRAILING_ACTION_MAX_WIDTH),
        enabled = enabled,
    ) {
        Text(
            text = text,
            style = WooTheme.text.labelLarge.emphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun WooSearchFieldDemo(
    modifier: Modifier = Modifier,
) {
    var searchValue by rememberSaveable { mutableStateOf("") }
    var searchWithClearValue by rememberSaveable { mutableStateOf("Search products") }
    var searchWithActionValue by rememberSaveable { mutableStateOf("") }
    var searchWithClearAndActionValue by rememberSaveable { mutableStateOf("Search products") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        WooSearchField(
            value = searchValue,
            onValueChange = { searchValue = it },
            placeholder = "Search products",
            modifier = Modifier.fillMaxWidth(),
        )
        WooSearchField(
            value = searchWithClearValue,
            onValueChange = { searchWithClearValue = it },
            onClearClick = { searchWithClearValue = "" },
            clearContentDescription = "Clear search",
            modifier = Modifier.fillMaxWidth(),
        )
        WooSearchField(
            value = searchWithActionValue,
            onValueChange = { searchWithActionValue = it },
            placeholder = "Search products",
            trailingActionText = "Cancel",
            onTrailingActionClick = { searchWithActionValue = "" },
            modifier = Modifier.fillMaxWidth(),
        )
        WooSearchField(
            value = searchWithClearAndActionValue,
            onValueChange = { searchWithClearAndActionValue = it },
            onClearClick = { searchWithClearAndActionValue = "" },
            clearContentDescription = "Clear search",
            trailingActionText = "Cancel",
            onTrailingActionClick = { searchWithClearAndActionValue = "" },
            modifier = Modifier.fillMaxWidth(),
        )
        WooSearchField(
            value = "Disabled search",
            onValueChange = {},
            enabled = false,
            onClearClick = {},
            clearContentDescription = "Clear search",
            trailingActionText = "Cancel",
            onTrailingActionClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooSearchFieldPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooSearchFieldDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

private val SEARCH_SHELL_HEIGHT = 64.dp
private val SEARCH_FIELD_HEIGHT = 40.dp
private val SEARCH_ACTION_TOUCH_TARGET_SIZE = 48.dp
private val SEARCH_TRAILING_ACTION_MAX_WIDTH = 120.dp
private val SEARCH_CLEAR_GLYPH_SIZE = 8.dp
private val SEARCH_CLEAR_ICON_PADDING = 4.dp
private val SEARCH_CLEAR_RESERVED_WIDTH = 28.dp
