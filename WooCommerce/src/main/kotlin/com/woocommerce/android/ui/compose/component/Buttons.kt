package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WCColoredButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        contentColor = colorResource(id = R.color.woo_white)
    ),
    rippleColor: Color = MaterialTheme.colors.primaryVariant,
    elevation: ButtonElevation? = null,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable RowScope.() -> Unit,
) {
    val contentColor by colors.contentColor(enabled = enabled)
    val rippleTheme = remember(rippleColor, contentColor) {
        object : RippleTheme {
            @Composable
            override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
                rippleColor,
                MaterialTheme.colors.isLight
            )

            @Composable
            override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
                rippleColor,
                MaterialTheme.colors.isLight
            )
        }
    }
    CompositionLocalProvider(LocalRippleTheme provides rippleTheme) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            elevation = elevation,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            modifier = modifier,
            shape = shape
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.subtitle2
            ) {
                content()
            }
        }
    }
}

@Composable
fun WCColoredButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        contentColor = colorResource(id = R.color.woo_white)
    )
) {
    WCColoredButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
        }
        Text(text = text)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
            trailingIcon()
        }
    }
}

@Composable
fun WCOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedBorder,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        border = border,
        modifier = modifier,
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.subtitle2
        ) {
            content()
        }
    }
}

@Composable
fun WCOutlinedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
) {
    WCOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
        }
        Text(text = text)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
            trailingIcon()
        }
    }
}

@Composable
fun WCSelectableChip(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    selectedButtonColors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        backgroundColor = colorResource(id = R.color.color_primary),
        contentColor = colorResource(id = R.color.woo_white)
    ),
    defaultButtonColors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        backgroundColor = Color.Transparent,
    ),
    isSelected: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(50),
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = if (isSelected) selectedButtonColors else defaultButtonColors,
        shape = shape,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
        }
        Text(text = text)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
            trailingIcon()
        }
    }
}

@Composable
fun WCTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors,
        content = content
    )
}

@Composable
fun WCTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allCaps: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors
    ) {
        Text(text = text.let { if (allCaps) it.toUpperCase(Locale.current) else it })
    }
}

@Composable
fun WCTextButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allCaps: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            )
            Text(text = text.let { if (allCaps) it.toUpperCase(Locale.current) else it })
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ButtonsPreview() {
    WooThemeWithBackground {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            WCColoredButton(onClick = {}) {
                Text("Button")
            }
            WCColoredButton(
                onClick = {},
                enabled = false
            ) {
                Text("Disabled Button")
            }
            WCColoredButton(
                onClick = {},
                text = "Button With icon",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(id = R.color.woo_white),
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorResource(id = R.color.woo_white),
                    )
                }
            )

            WCOutlinedButton(onClick = {}) {
                Text(text = "Outlined Button")
            }
            WCOutlinedButton(onClick = {}, enabled = false) { Text(text = "Disabled Outlined Button") }

            WCOutlinedButton(
                onClick = {},
                text = "Outlined Button with icon",
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            WCTextButton(onClick = {}) {
                Text(text = "Text button")
            }
            WCTextButton(onClick = {}, text = "Text Button")
            WCTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                text = "Text Button",
                icon = Icons.Default.Add
            )

            WCSelectableChip(
                onClick = {},
                text = "Selectable Chip: selected",
                isSelected = true
            )
            WCSelectableChip(
                onClick = {},
                text = "Selectable Chip: not selected",
                isSelected = false
            )
        }
    }
}
