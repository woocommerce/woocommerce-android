package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCColoredButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.wcColoredButtonColors(),
    rippleColor: Color = colorResource(R.color.color_primary_variant),
    elevation: ButtonElevation? = null,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable RowScope.() -> Unit,
) {
    val rippleConfiguration = RippleConfiguration(color = rippleColor)

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            elevation = elevation,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            modifier = modifier,
            shape = shape,
            content = content
        )
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
    loading: Boolean = false,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.wcColoredButtonColors()
) {
    WCColoredButton(
        onClick = { if (!loading) onClick() },
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors
    ) {
        Box {
            if (loading) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (loading) 0f else 1f)
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
    }
}

@Composable
fun WCOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.wcOutlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        border = border,
        modifier = modifier,
        content = content
    )
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
    colors: ButtonColors = ButtonDefaults.wcOutlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled)
) {
    WCOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        colors = colors,
        border = border
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
fun WCRemoveButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.wcOutlinedButtonColors().copy(
        contentColor = MaterialTheme.colorScheme.error
    )
) {
    WCOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, colorResource(id = R.color.woo_red_50)),
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
        containerColor = colorResource(id = R.color.primary_colored_button_background),
        contentColor = colorResource(id = R.color.woo_white)
    ),
    defaultButtonColors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
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
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        disabledContentColor = colorResource(id = R.color.color_on_surface_medium),
    ),
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.small,
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
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        disabledContentColor = colorResource(id = R.color.color_on_surface_medium),
    ),
) {
    WCTextButton(
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
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        disabledContentColor = colorResource(id = R.color.color_on_surface_medium),
    ),
) {
    WCTextButton(
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

@Composable
private fun ButtonDefaults.wcColoredButtonColors(): ButtonColors {
    return buttonColors(
        containerColor = colorResource(id = R.color.primary_colored_button_background),
        // For now, our onPrimary color is black for dark theme, which seems to be a mistake.
        // We are using white here to match the design, until confirming if we need to change the onPrimary color.
        contentColor = colorResource(id = R.color.woo_white)
    )
}

@Composable
private fun ButtonDefaults.wcOutlinedButtonColors(): ButtonColors {
    return outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface
    )
}

@LightDarkThemePreviews
@Composable
private fun ButtonsPreview() {
    WooThemeWithBackground {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .safeContentPadding()
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

            WCRemoveButton(
                onClick = {},
                text = "Remove",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
